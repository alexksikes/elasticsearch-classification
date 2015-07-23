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

import org.elasticsearch.action.support.broadcast.BroadcastOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.util.Map;

/**
 * 
 */
public class ClassifyRequestBuilder extends BroadcastOperationRequestBuilder<ClassifyRequest, ClassifyResponse, ClassifyRequestBuilder> {

    /**
     * Constructs evaluate classifier request
     */
    public ClassifyRequestBuilder(ElasticsearchClient client, ClassifyAction action) {
        super(client, action, new ClassifyRequest());
    }

    /**
     * Constructs evaluate classifier request on the given train index and type
     */
    public ClassifyRequestBuilder(ElasticsearchClient client, ClassifyAction action, String trainIndex, String trainType) {
        super(client, action, new ClassifyRequest(trainIndex, trainType));
    }

    /**
     * Sets the index on which to train the classifier
     *
     * @param trainIndex the index on which to train the classifier
     * @return this request
     */
    public ClassifyRequestBuilder setTrainIndex(String trainIndex) {
        request.trainIndex(trainIndex);
        return this;
    }

    /**
     * Sets the document type type on which to train the classifier
     *
     * @param trainType the document type type on which to train the classifier
     * @return this request
     */
    public ClassifyRequestBuilder setTrainType(String trainType) {
        request.trainType(trainType);
        return this;
    }

    /**
     * Sets the field name to train on
     *
     * @param textField field name to train on
     * @return this request
     */
    public ClassifyRequestBuilder setTextField(String textField) {
        request.textField(textField);
        return this;
    }

    /**
     * Sets field containing the class
     *
     * @param classField the field containing the class
     * @return this request
     */
    public ClassifyRequestBuilder setClassField(String classField) {
        request.classField(classField);
        return this;
    }

    /**
     * Sets the text on which the classifier will be evaluated
     *
     * @param evalOn the text on which the classifier will be evaluated
     * @return this request
     */
    public ClassifyRequestBuilder setEvalOn(String evalOn) {
        request.evalOn(evalOn);
        return this;
    }

    /**
     * Sets the query to filter which documents use for training
     *
     * @param trainQuery the query to filter which documents use for training
     * @return this request
     */
    public ClassifyRequestBuilder setTrainQuery(BytesReference trainQuery) {
        request.trainQuery(trainQuery);
        return this;
    }

    /**
     * Sets the query to filter which documents use for training
     *
     * @param trainQuery the query to filter which documents use for training
     * @return this request
     */
    public ClassifyRequestBuilder setTrainQuery(XContentBuilder trainQuery) {
        request.trainQuery(trainQuery);
        return this;
    }

    /**
     * Sets the query to filter which documents use for training
     *
     * @param trainQuery the query to filter which documents use for training
     * @return this request
     */
    public ClassifyRequestBuilder setTrainQuery(Map trainQuery) {
        request.trainQuery(trainQuery);
        return this;
    }

    /**
     * Sets analyzer to process the text field
     *
     * @param analyzer analyzer to process the text field
     * @return this request
     */
    public ClassifyRequestBuilder setAnalyzer(String analyzer) {
        request.analyzer(analyzer);
        return this;
    }

    /**
     * Sets the type of model to use
     *
     * @param modelType type of model to use
     * @return this request
     */
    public ClassifyRequestBuilder setModelType(String modelType) {
        request.modelType(modelType);
        return this;
    }

    /**
     * Sets classifier specific settings
     *
     * @param settings classifier specific settings
     * @return this request
     */
    public ClassifyRequestBuilder setModelSettings(Settings settings) {
        request.modelSettings(settings);
        return this;
    }

    /**
     * Sets classifier specific settings
     *
     * @param settings classifier specific settings
     * @return this request
     */
    public ClassifyRequestBuilder setModelSettings(Settings.Builder settings) {
        request.modelSettings(settings);
        return this;
    }

    /**
     * Sets classifier specific settings
     *
     * @param settings classifier specific settings
     * @return this request
     */
    public ClassifyRequestBuilder setModelSettings(Map<String, Object> settings) {
        request.modelSettings(settings);
        return this;
    }

    /**
     * Sets classifier specific settings
     *
     * @param settings classifier specific settings in json, yaml or properties format
     * @return this request
     */
    public ClassifyRequestBuilder setModelSettings(String settings) {
        request.modelSettings(settings);
        return this;
    }


    /**
     * A comma separated list of routing values to control the shards the action will be executed on.
     */
    public ClassifyRequestBuilder setRouting(String routing) {
        request.routing(routing);
        return this;
    }

    /**
     * The routing values to control the shards that the action will be executed on.
     */
    public ClassifyRequestBuilder setRouting(String... routing) {
        request.routing(routing);
        return this;
    }
}
