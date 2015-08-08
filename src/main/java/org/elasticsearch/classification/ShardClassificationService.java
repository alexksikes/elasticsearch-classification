/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.classification;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.classification.*;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.classify.ClassifyRequest;
import org.elasticsearch.action.classify.ClassifyRequest.ModelTypes;
import org.elasticsearch.action.classify.ClassifyResult;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.core.*;
import org.elasticsearch.index.query.IndexQueryParserService;
import org.elasticsearch.index.shard.AbstractIndexShardComponent;
import org.elasticsearch.index.shard.IndexShard;

import java.io.IOException;

/**
 */
public class ShardClassificationService extends AbstractIndexShardComponent {

    public static final String DEFAULT_MODEL_TYPE = "simple_naive_bayes";
    
    public static final Double DEFAULT_BOOLEAN_PERCEPTRON_THRESHOLD = null; // automatic
    public static final int DEFAULT_BOOLEAN_PERCEPTRON_BATCH_SIZE = 1;

//    public static final int DEFAULT_KNN_K = 3;
//    public static final int DEFAULT_KNN_MIN_DOC_FREQ = 0;
//    public static final int DEFAULT_KNN_MIN_TERM_FREQ = 0;

    private IndexShard indexShard;
    private IndexQueryParserService queryParser;

    // Unfortunately it does not seem possible to bind shard services in a plugin
    public ShardClassificationService(IndexShard indexShard) {
        super(indexShard.shardId(), indexShard.indexSettings());
        this.indexShard = indexShard;
        this.queryParser = indexShard.indexService().queryParserService();
    }

    public ClassifyResult evaluate(ClassifyRequest request) throws IOException {
        // get the classifier
        Classifier classifier;
        if (request.modelType() == null) {
            classifier = getClassifier(DEFAULT_MODEL_TYPE, request.modelSettings());
        } else {
            classifier = getClassifier(request.modelType(), request.modelSettings());
        }
        // train the classifier
        trainClassifier(classifier, request);  // boolean perceptron is always retrained for now
        // evaluate the classifier
        ClassificationResult<Object> result = classifier.assignClass(request.evalOn());

        // convert the assigned class to the proper field type value
        Object assignedClass = result.getAssignedClass();
        if (assignedClass instanceof BytesRef) {
            assignedClass = convertBytesRefToType(request.classField(), (BytesRef) assignedClass);
        }
        return new ClassifyResult(assignedClass, result.getScore());
    }

    private void trainClassifier(Classifier classifier, ClassifyRequest request) {
        // parse the query and get analyzer at field if possible
        Query luceneQuery = queryParser.parse(request.trainQuery()).query();
        Analyzer analyzer = getAnalyzerAtField(request.textField());

        // call train method
        final Engine.Searcher searcher = indexShard.acquireSearcher("classify");
        try {
            LeafReader leafReader = SlowCompositeReaderWrapper.wrap(indexShard.acquireSearcher("classify").reader());
            classifier.train(leafReader, request.textField(), request.classField(), analyzer, luceneQuery);
        } catch (Throwable ex) {
            throw new ElasticsearchException("failed to train model", ex);
        } finally {
            searcher.close();
        }
    }

    private Classifier getClassifier(String modelType, Settings settings) {
        switch (modelType) {
            case ModelTypes.SIMPLE_NAIVE_BAYES:
                return new SimpleNaiveBayesClassifier();
            case ModelTypes.CACHING_NAIVE_BAYES:
                return new CachingNaiveBayesClassifier();
            case ModelTypes.BOOLEAN_PERCEPTRON:
                if (settings != null && settings.getAsMap().size() != 0) {
                    return new BooleanPerceptronClassifier(
                            settings.getAsDouble("threshold", DEFAULT_BOOLEAN_PERCEPTRON_THRESHOLD),
                            settings.getAsInt("batch_size", DEFAULT_BOOLEAN_PERCEPTRON_BATCH_SIZE));
                }
                return new BooleanPerceptronClassifier();
//            case "knn":
//                if (settings != null && settings.getAsMap().size() != 0) {
//                    return new KNearestNeighborClassifier(
//                            settings.getAsInt("k", DEFAULT_KNN_K), 
//                            settings.getAsInt("min_doc_freq", DEFAULT_KNN_MIN_DOC_FREQ)
//                            settings.getAsInt("min_term_freq", DEFAULT_KNN_MIN_TERM_FREQ));
//                }
//                return new KNearestNeighborClassifier(DEFAULT_KNN_K);
        }
        throw new IllegalArgumentException("unknown model type [" + modelType + "]");
    }
    
    private Analyzer getAnalyzerAtField(String field) {
        MapperService mapperService = this.indexShard.mapperService();
        Analyzer analyzer = mapperService.analysisService().analyzer(field);
        if(analyzer == null) {
            analyzer = mapperService.analysisService().defaultAnalyzer();
        }
        return analyzer;
    }

    private Object convertBytesRefToType(String fieldName, BytesRef bytesRef) {
        MappedFieldType fieldType = indexShard.mapperService().smartNameFieldType(fieldName);
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
}
