package org.elasticsearch.classification;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BooleanPerceptronClassifier extends org.apache.lucene.classification.BooleanPerceptronClassifier {

    public BooleanPerceptronClassifier() {
        super();
    }

    public BooleanPerceptronClassifier(Double threshold, Integer batchSize) {
        super(threshold, batchSize);
    }

    @Override
    public void train(LeafReader leafReader, String[] textFieldNames, String classFieldName, Analyzer analyzer, Query query) throws IOException {
        if (textFieldNames.length == 1) {
            super.train(leafReader, textFieldNames[0], classFieldName, analyzer, query);
        } else {
            throw new IOException("training with multiple fields not supported by boolean perceptron classifier");
        }
    }

    @Override
    public List<ClassificationResult<Boolean>> getClasses(String text) throws IOException {
        List<ClassificationResult<Boolean>> result = new ArrayList<>();
        result.add(assignClass(text));
        return result;
    }

    @Override
    public List<ClassificationResult<Boolean>> getClasses(String text, int max) throws IOException {
        return getClasses(text);
    }

}
