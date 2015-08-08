package org.elasticsearch.action.classify;

import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.core.*;

import java.io.IOException;
import java.util.*;

public class ClassifyResult implements Streamable, Iterable<ClassificationResult>, ToXContent {

    private Map<Object, ClassificationResult> results;

    public ClassifyResult() {
        this.results = new HashMap<>();
    }

    public ClassifyResult(List<ClassificationResult> results, MappedFieldType fieldType) {
        this.results = new HashMap<>(results.size());
        // maybe only do this conversion when rendering the results?
        for (ClassificationResult result : results) {
            Object assignedClass = result.getAssignedClass();
            if (assignedClass instanceof BytesRef) {
                assignedClass = convertBytesRefToValue(fieldType, (BytesRef) assignedClass);
            }
            this.add(new ClassificationResult(assignedClass, result.getScore()));
        }
    }

    private void add(ClassificationResult classificationResult) {
        this.add(classificationResult, 1);
    }

    // at some point we should have our own ClassifcationResult class to modify in place
    private void add(ClassificationResult classificationResult, double factor) {
        final Object assignedClass = classificationResult.getAssignedClass();
        double score = factor * classificationResult.getScore();
        if (results.containsKey(assignedClass)) {
            score = results.get(assignedClass).getScore() + score;
        }
        results.put(assignedClass, new ClassificationResult(assignedClass, score));
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        int size = in.readVInt();
        results = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            Object assignedClass = in.readGenericValue();
            results.put(assignedClass, new ClassificationResult(assignedClass, in.readDouble()));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(results.size());
        for (ClassificationResult result : this) {
            out.writeGenericValue(result.getAssignedClass());
            out.writeDouble(result.getScore());
        }
    }

    @Override
    public Iterator<ClassificationResult> iterator() {
        if (results == null) {
            return Collections.emptyIterator();
        }
        return results.values().iterator();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        List<ClassificationResult> values = new ArrayList<>(results.values());
        Collections.sort(values);
        values.subList(0, 1);  // until we implement top_n
        for (ClassificationResult result : values) {
            builder.startObject();
            builder.field("value", result.getAssignedClass());
            builder.field("score", result.getScore());
            builder.endObject();
        }
        return builder;
    }

    private static Object convertBytesRefToValue(MappedFieldType fieldType, BytesRef bytesRef) {
        switch(fieldType.typeName()) {
            case FloatFieldMapper.CONTENT_TYPE:
                return NumericUtils.sortableIntToFloat(NumericUtils.prefixCodedToInt(bytesRef));
            case DoubleFieldMapper.CONTENT_TYPE:
                return NumericUtils.sortableLongToDouble(NumericUtils.prefixCodedToLong(bytesRef));
            case ShortFieldMapper.CONTENT_TYPE:
            case IntegerFieldMapper.CONTENT_TYPE:
                return NumericUtils.prefixCodedToInt(bytesRef);
            case LongFieldMapper.CONTENT_TYPE:
                return NumericUtils.prefixCodedToLong(bytesRef);
            case BooleanFieldMapper.CONTENT_TYPE:
                return fieldType.value(bytesRef);
            default:
                return bytesRef.utf8ToString();
        }
    }

    public static ClassifyResult fromAverage(List<ClassifyResult> classifyResults) {
        int size = classifyResults.size();
        ClassifyResult aveResults = new ClassifyResult();
        for (ClassifyResult classifyResult : classifyResults) {
            for (ClassificationResult classificationResult : classifyResult) {
                aveResults.add(classificationResult, 1.0 / size);
            }
        }
        return aveResults;
    }
}
