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

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

import java.io.IOException;
import java.util.List;

/**
 * 
 */
public class ClassifyResponse extends BroadcastResponse implements ToXContent {

    static final class Fields {
        static final XContentBuilderString TOOK = new XContentBuilderString("took");
        static final XContentBuilderString TEXT = new XContentBuilderString("text");
        static final XContentBuilderString CLASS = new XContentBuilderString("class");
        static final XContentBuilderString SCORES = new XContentBuilderString("scores");
        static final XContentBuilderString FAILURES = new XContentBuilderString("failures");
    }
    
    private String evalOn;
    private String classField;
    private ClassifyResult classifyResult;
    private long tookInMillis;

    public ClassifyResponse() {
    }

    public ClassifyResponse(String evalOn, String classField, ClassifyResult classifyResult,
                            int totalShards, int successfulShards, int failedShards,
                            List<ShardOperationFailedException> shardFailures, long tookInMillis) {
        super(totalShards, successfulShards, failedShards, shardFailures);
        this.evalOn = evalOn;
        this.classField = classField;
        this.classifyResult = classifyResult;
        this.tookInMillis = tookInMillis;
    }

    public String getEvalOn() {
        return this.evalOn;
    }

    public String getClassField() {
        return this.classField;
    }

    public ClassifyResult getClassifyResult() {
        return this.classifyResult;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        evalOn = in.readString();
        classField = in.readString();
        classifyResult = ClassifyResult.readClassifyResultFrom(in);
        tookInMillis = in.readVLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(evalOn);
        out.writeString(classField);
        classifyResult.writeTo(out);
        out.writeVLong(tookInMillis);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field(Fields.TOOK, tookInMillis);
        builder.field(Fields.TEXT, evalOn);
        builder.field(Fields.CLASS, classField);
        buildScores(builder, params);

        if (this.getShardFailures() != null && this.getShardFailures().length != 0) {
            buildShardFailures(builder, params);
        }
        
        return builder;
    }

    private void buildScores(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(Fields.SCORES);
        builder.value(classifyResult);
        builder.endObject();
    }

    private void buildShardFailures(XContentBuilder builder, Params params) throws IOException {
        builder.startArray(Fields.FAILURES);
        for (ShardOperationFailedException shardFailure : this.getShardFailures()) {
            builder.startObject();
            shardFailure.toXContent(builder, params);
            builder.endObject();
        }
        builder.endArray();
    }
}
