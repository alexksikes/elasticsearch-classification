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

package org.elasticsearch.action.classify;

import org.elasticsearch.ElasticsearchGenerationException;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.support.broadcast.BroadcastRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.action.ValidateActions.addValidationError;
import static org.elasticsearch.common.settings.Settings.Builder.EMPTY_SETTINGS;
import static org.elasticsearch.common.settings.Settings.readSettingsFromStream;
import static org.elasticsearch.common.settings.Settings.writeSettingsToStream;

/**
 * 
 */
public class ClassifyRequest extends BroadcastRequest<ClassifyRequest> {

    public static class ModelTypes {
        public static final String SIMPLE_NAIVE_BAYES = "simple_naive_bayes";
        public static final String CACHING_NAIVE_BAYES = "caching_naive_bayes";
        public static final String BOOLEAN_PERCEPTRON = "boolean_perceptron";
        public static final String KNN = "knn";
    }

    public static int DEFAULT_TOP_N = 3;

    private String trainIndex;

    private String trainType;

    private String[] textFields;

    private String classField;
    
    private String evalOn;

    private BytesReference trainQuery;

    private String analyzer;

    private String modelType;

    private Settings modelSettings = EMPTY_SETTINGS;

    private String routing;

    private int topN = DEFAULT_TOP_N;

    long nowInMillis;
    
    ClassifyRequest() {
        super();
    }

    /**
     * Constructs a new train classifier request
     */
    public ClassifyRequest(String trainIndex, String trainType) {
        super(new String[]{trainIndex});
        this.trainIndex = trainIndex;
        this.trainType = trainType;
    }

    /**
     * Returns index on which to train the classifier
     *
     * @return the index on which to train the classifier
     */
    public String trainIndex() {
        return trainIndex;
    }

    /**
     * Sets the index on which to train the classifier
     *
     * @param trainIndex the index on which to train the classifier
     * @return this request
     */
    public ClassifyRequest trainIndex(String trainIndex) {
        this.trainIndex = trainIndex;
        return this;
    }

    /**
     * Returns the document type type on which to train the classifier
     *
     * @return the document type on which to train the classifier
     */
    public String trainType() {
        return trainType;
    }

    /**
     * Sets the document type type on which to train the classifier
     *
     * @param trainType the document type type on which to train the classifier
     * @return this request
     */
    public ClassifyRequest trainType(String trainType) {
        this.trainType = trainType;
        return this;
    }

    /**
     * Returns the field to train on
     *
     * @return the field to train on
     */
    public String[] textFields() {
        return textFields;
    }

    /**
     * Sets the field name to train on
     *
     * @param textField field name to train on
     * @return this request
     */
    public ClassifyRequest textFields(String... textField) {
        this.textFields = textField;
        return this;
    }

    /**
     * Returns field containing the class
     *
     * @return the field containing the class
     */
    public String classField() {
        return classField;
    }

    /**
     * Sets field containing the class
     *
     * @param classField the field containing the class
     * @return this request
     */
    public ClassifyRequest classField(String classField) {
        this.classField = classField;
        return this;
    }

    /**
     * Returns the text on which the classifier will be evaluated
     *
     * @return the text on which the classifier will be evaluated
     */
    public String evalOn() {
        return evalOn;
    }

    /**
     * Sets the text on which the classifier will be evaluated
     *
     * @param evalOn the text on which the classifier will be evaluated
     * @return this request
     */
    public ClassifyRequest evalOn(String evalOn) {
        this.evalOn = evalOn;
        return this;
    }

    /**
     * Returns the query to filter which documents used for training
     *
     * @return the query to filter which documents used for training
     */
    public BytesReference trainQuery() {
        return trainQuery;
    }
    
    /**
     * Returns the query to filter which documents used for training
     *
     * @return the query to filter which documents used for training
     */
    public Map<String, Object> trainQueryAsMap() {
        return XContentHelper.convertToMap(trainQuery, false).v2();
    }

    /**
     * Sets the query to filter which documents use for training
     *
     * @param trainQuery the query to filter which documents use for training
     * @return this request
     */
    public ClassifyRequest trainQuery(BytesReference trainQuery) {
        this.trainQuery = trainQuery;
        return this;
    }

    /**
     * Sets the query to filter which documents use for training
     *
     * @param trainQuery the query to filter which documents use for training
     * @return this request
     */
    public ClassifyRequest trainQuery(XContentBuilder trainQuery) {
        this.trainQuery = trainQuery.bytes();
        return this;
    }

    /**
     * Sets the query to filter which documents use for training
     *
     * @param trainQuery the query to filter which documents use for training
     * @return this request
     */
    public ClassifyRequest trainQuery(Map trainQuery) throws ElasticsearchGenerationException {
        try {
            XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
            builder.map(trainQuery);
            return trainQuery(builder);
        } catch (IOException e) {
            throw new ElasticsearchGenerationException("Failed to generate [" + trainQuery + "]", e);
        }
    }
    
    /**
     * Returns analyzer to process the text field
     *
     * @return analyzer to process the text field
     */
    public String analyzer() {
        return analyzer;
    }

    /**
     * Sets analyzer to process the text field
     *
     * @param analyzer analyzer to process the text field
     * @return this request
     */
    public ClassifyRequest analyzer(String analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    /**
     * Returns the type of model to use
     *
     * @return the type of model to use
     */
    public String modelType() {
        return modelType;
    }

    /**
     * Sets the type of model to use
     *
     * @param modelType type of model to use
     * @return this request
     */
    public ClassifyRequest modelType(String modelType) {
        if (!modelType.equals(ModelTypes.SIMPLE_NAIVE_BAYES) && !modelType.equals(ModelTypes.CACHING_NAIVE_BAYES) && 
                !modelType.equals(ModelTypes.BOOLEAN_PERCEPTRON) && !modelType.equals(ModelTypes.KNN)) {
            throw new IllegalArgumentException("unknown model type [" + modelType + "]");
        }
        this.modelType = modelType;
        return this;
    }

    /**
     * Returns classifier specific settings type
     *
     * @return the classifier specific settings
     */
    public Settings modelSettings() {
        return modelSettings;
    }

    /**
     * Sets classifier specific settings
     *
     * @param settings classifier specific settings
     * @return this request
     */
    public ClassifyRequest modelSettings(Settings settings) {
        this.modelSettings = settings;
        return this;
    }

    /**
     * Sets classifier specific settings
     *
     * @param settings classifier specific settings
     * @return this request
     */
    public ClassifyRequest modelSettings(Settings.Builder settings) {
        this.modelSettings = settings.build();
        return this;
    }

    /**
     * Sets classifier specific settings
     *
     * @param settings classifier specific settings
     * @return this request
     */
    public ClassifyRequest modelSettings(Map<String, Object> settings) {
        try {
            XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
            builder.map(settings);
            return modelSettings(builder.string());
        } catch (IOException e) {
            throw new ElasticsearchGenerationException("Failed to generate [" + settings + "]", e);
        }
    }
    
    /**
     * Sets classifier specific settings
     *
     * @param settings classifier specific settings in json, yaml or properties format
     * @return this request
     */
    public ClassifyRequest modelSettings(String settings) {
        this.modelSettings = Settings.settingsBuilder().loadFromSource(settings).build();
        return this;
    }

    public String routing() {
        return this.routing;
    }

    public ClassifyRequest routing(String routing) {
        this.routing = routing;
        return this;
    }

    public ClassifyRequest routing(String... routings) {
        this.routing = Strings.arrayToCommaDelimitedString(routings);
        return this;
    }

    public int topN() {
        return this.topN;
    }

    public ClassifyRequest topN(int topN) {
        this.topN = topN;
        return this;
    }

    /**
     * Parses model definition.
     *
     * @param source model definition
     */
    public ClassifyRequest source(Map source) throws IOException {
        Map<String, Object> sourceMap = source;
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String name = entry.getKey();
            if (name.equals("field")) {
                textFields(entry.getValue().toString());
            } else if (name.equals("fields")) {
                if (!(entry.getValue() instanceof List)) {
                    throw new IllegalArgumentException("malformed fields, should be an array of strings");
                }
                textFields(((List<String>) entry.getValue()).toArray(new String[0]));
            } else if (name.equals("class")) {
                classField(entry.getValue().toString());
            } else if (name.equals("text")) {
                evalOn(entry.getValue().toString());
            } else if (name.equals("query")) {
                if (!(entry.getValue() instanceof Map)) {
                    throw new IllegalArgumentException("malformed query, should include an inner object");
                }
                trainQuery((Map<String, Object>) entry.getValue());
            } else if (name.equals("analyzer")) {
                analyzer(entry.getValue().toString());
            } else if (name.equals("model")) {
                modelType(entry.getValue().toString());
            } else if (name.equals("settings")) {
                if (!(entry.getValue() instanceof Map)) {
                    throw new IllegalArgumentException("malformed model settings section, should include an inner object");
                }
                modelSettings((Map<String, Object>) entry.getValue());
            } else if (name.equals("top_n")) {
                topN((int) entry.getValue());
            } else {
                throw new IllegalArgumentException("unknown parameter [" + name + "]");
            }
        }
        return this;
    }

    /**
     * Parses model definition.
     *
     * @param source model definition
     */
    public ClassifyRequest source(XContentBuilder source) {
        return source(source.bytes());
    }

    /**
     * Parses model definition.
     * JSON, Smile and YAML formats are supported
     *
     * @param source model definition
     */
    public ClassifyRequest source(BytesReference source) {
        try (XContentParser parser = XContentFactory.xContent(source).createParser(source)) {
            return source(parser.mapOrdered());
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to parse template source", e);
        }
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = super.validate();
        if (textFields == null || textFields.length == 0) {
            validationException = addValidationError("name of the field used to compare documents is either missing or empty", validationException);
        }
        if (classField == null) {
            validationException = addValidationError("name of the field containing the class assigned to documents is missing", validationException);
        }
        if (trainIndex == null) {
            validationException = addValidationError("index on which to train the classifier is missing", validationException);
        }
        if (trainType == null) {
            validationException = addValidationError("type on which to train the classifier is missing", validationException);
        }
        if (evalOn == null) {
            validationException = addValidationError("text to be evaluated is missing", validationException);
        }
        return validationException;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        textFields = in.readStringArray();
        classField = in.readString();
        trainIndex = in.readString();
        trainType = in.readString();
        trainQuery = in.readBytesReference();
        analyzer = in.readOptionalString();
        modelType = in.readOptionalString();
        modelSettings = readSettingsFromStream(in);
        topN = in.readVInt();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeStringArray(textFields);
        out.writeString(classField);
        out.writeString(trainIndex);
        out.writeString(trainType);
        out.writeBytesReference(trainQuery);
        out.writeOptionalString(analyzer);
        out.writeOptionalString(modelType);
        writeSettingsToStream(modelSettings, out);
        out.writeVInt(topN);
    }
}
