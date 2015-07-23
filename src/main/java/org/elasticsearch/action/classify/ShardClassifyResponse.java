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

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.support.broadcast.BroadcastShardResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.shard.ShardId;

import java.io.IOException;

/**
 *
 */
class ShardClassifyResponse extends BroadcastShardResponse {

    private BytesRef assignedClass;
    private double score;

    ShardClassifyResponse() {

    }

    ShardClassifyResponse(ShardId shardId, BytesRef assignedClass, double score) {
        super(shardId);
        this.assignedClass = assignedClass;
        this.score = score;
    }

    public BytesRef getAssignedClass() {
        return this.assignedClass;
    }

    public double getScore() {
        return this.score;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        assignedClass = in.readBytesRef();
        score = in.readDouble();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeBytesRef(assignedClass);
        out.writeDouble(score);
    }
}
