package org.elasticsearch.action.classify;

import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.classification.Classifier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;

public class ClassifyResult extends ClassificationResult<Object> implements Writeable<ClassifyResult>, ToXContent {

    private static ClassifyResult PROTOTYPE = new ClassifyResult(null, -1);

    /**
     * Constructor
     *
     * @param assignedClass the class <code>T</code> assigned by a {@link Classifier}
     * @param score         the score for the assignedClass as a <code>double</code>
     */
    public ClassifyResult(Object assignedClass, double score) {
        super(assignedClass, score);
    }

    public static ClassifyResult readClassifyResultFrom(StreamInput in) throws IOException {
        return PROTOTYPE.readFrom(in);
    }

    @Override
    public ClassifyResult readFrom(StreamInput in) throws IOException {
        return new ClassifyResult(in.readGenericValue(), in.readDouble());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeGenericValue(getAssignedClass());
        out.writeDouble(getScore());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field("value", getAssignedClass());
        builder.field("score", getScore());
        return builder;
    }
}
