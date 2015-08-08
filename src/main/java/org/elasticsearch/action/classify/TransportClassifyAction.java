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

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import org.apache.lucene.classification.ClassificationResult;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.TransportBroadcastAction;
import org.elasticsearch.classification.ShardClassificationService;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.search.controller.SearchPhaseController;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static com.google.common.collect.Lists.newArrayList;

/**
 * 
 */
public class TransportClassifyAction extends TransportBroadcastAction<ClassifyRequest, ClassifyResponse, ShardClassifyRequest, ShardClassifyResponse> {

    private final IndicesService indicesService;

    @Inject
    public TransportClassifyAction(Settings settings, ThreadPool threadPool, ClusterService clusterService, TransportService transportService,
                                   ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver, IndicesService indicesService, SearchPhaseController searchPhaseController) {
        super(settings, ClassifyAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                ClassifyRequest.class, ShardClassifyRequest.class, ThreadPool.Names.SEARCH);
        this.indicesService = indicesService;
    }

    @Override
    protected void doExecute(ClassifyRequest request, ActionListener<ClassifyResponse> listener) {
        request.nowInMillis = System.currentTimeMillis();
        super.doExecute(request, listener);
    }

    @Override
    protected ShardClassifyRequest newShardRequest(int numShards, ShardRouting shard, ClassifyRequest request) {
        return new ShardClassifyRequest(shard, request);
    }

    @Override
    protected ShardClassifyResponse newShardResponse() {
        return new ShardClassifyResponse();
    }

    @Override
    protected GroupShardsIterator shards(ClusterState clusterState, ClassifyRequest request, String[] concreteIndices) {
        Map<String, Set<String>> routingMap = indexNameExpressionResolver.resolveSearchRouting(clusterState, request.routing(), request.indices());
        return clusterService.operationRouting().searchShards(clusterState, concreteIndices, routingMap, null);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, ClassifyRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, ClassifyRequest countRequest, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.READ, concreteIndices);
    }

    @Override
    protected ClassifyResponse newResponse(ClassifyRequest request, AtomicReferenceArray shardsResponses, ClusterState clusterState) {
        int successfulShards = 0;
        int failedShards = 0;
        List<ShardOperationFailedException> shardFailures = null;

        // count the assigned classes and keep track of the average scores
        Multiset<Object> assignedClasses = LinkedHashMultiset.create();
        Map<Object, Double> aveScores = new HashMap<>();
        for (int i = 0; i < shardsResponses.length(); i++) {
            Object shardResponse = shardsResponses.get(i);
            if (shardResponse == null) {
                // simply ignore non active shards
            } else if (shardResponse instanceof BroadcastShardOperationFailedException) {
                failedShards++;
                if (shardFailures == null) {
                    shardFailures = newArrayList();
                }
                shardFailures.add(new DefaultShardOperationFailedException((BroadcastShardOperationFailedException) shardResponse));
            } else {
                ShardClassifyResponse resp = (ShardClassifyResponse) shardResponse;
                ClassificationResult result = resp.getClassificationResult();

                Object assignedClass = result.getAssignedClass();
                assignedClasses.add(assignedClass);
                Double score = aveScores.get(assignedClass);
                if (score != null) {
                    aveScores.put(assignedClass, score + result.getScore());
                } else {
                    aveScores.put(assignedClass, result.getScore());
                }
                successfulShards++;
            }
        }
        
        // now take a majority vote and return as a score the average score of the winners
        Object winnerClass = null;
        int winnerCount = 1;
        for (Multiset.Entry<Object> entry : assignedClasses.entrySet()) {
            if (entry.getCount() > winnerCount) {
                winnerCount = entry.getCount();
                winnerClass = entry.getElement();
            }
        }
        if (winnerClass == null) {
            throw new ElasticsearchException("unable to evaluate the model, no winner class!");
        }
        double winnerAveScore = 1.0 * aveScores.get(winnerClass) / winnerCount;

        return new ClassifyResponse(request.evalOn(), request.classField(), new ClassificationResult(winnerClass, winnerAveScore),
                shardsResponses.length(), successfulShards, failedShards, shardFailures, buildTookInMillis(request));
    }

    @Override
    protected ShardClassifyResponse shardOperation(ShardClassifyRequest request) {
        ShardId shardId = request.shardId();
        IndexService indexService = indicesService.indexServiceSafe(shardId.getIndex());
        IndexShard indexShard = indexService.shardSafe(shardId.id());
        
        ShardClassificationService classificationService = new ShardClassificationService(indexShard);

        ClassificationResult classificationResult = null;
        try {
            classificationResult = classificationService.evaluate(request.getEvaluateClassifierRequest());
        } catch (IOException e) {
            throw new ElasticsearchException("Unable to evaluate the model at the shard!", e);
        }
        return new ShardClassifyResponse(request.shardId(), classificationResult);
    }

    /**
     * Builds how long it took to execute the dfs request.
     */
    protected final long buildTookInMillis(ClassifyRequest request) {
        // protect ourselves against time going backwards
        // negative values don't make sense and we want to be able to serialize that thing as a vLong
        return Math.max(1, System.currentTimeMillis() - request.nowInMillis);
    }
}
