package com.jn.esmvc.service.impl;

import com.jn.esmvc.model.AbstractESModel;
import com.jn.esmvc.model.utils.ESModels;
import com.jn.esmvc.service.AbstractESModelService;
import com.jn.esmvc.service.ESModelCRUDService;
import com.jn.langx.util.Preconditions;
import com.jn.langx.util.collection.Collects;
import com.jn.langx.util.function.Consumer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jn.esmvc.service.util.ESRequests.asModel;
import static com.jn.esmvc.service.util.ESRequests.logRequestWhenFail;
import static org.elasticsearch.rest.RestStatus.CREATED;
import static org.elasticsearch.rest.RestStatus.OK;

public class ESModelCRUDServiceImpl<MODEL extends AbstractESModel> extends AbstractESModelService<MODEL> implements ESModelCRUDService<MODEL> {
    private static final Logger logger = LoggerFactory.getLogger(ESModelCRUDServiceImpl.class);

    @Override
    public String add(MODEL model) throws IOException {
        Preconditions.checkNotNull(model);
        String index = ESModels.getIndex(model.getClass());
        String type = getClient().isIgnoreIndexType() ? null : ESModels.getType(model.getClass());
        String id = ESModels.getId(model);
        IndexRequest request = new IndexRequest(index, type, id);
        request.source(json.toJson(model), XContentType.JSON);
        IndexResponse response;
        try {
            response = client.index(request, null);
            RestStatus status = response.status();
            if (status == CREATED || status == OK) {
                return id;
            }
        } catch (Throwable ex) {
            logRequestWhenFail(logger, request, ex);
            throw ex;
        }

        return id;
    }

    @Override
    public MODEL getById(String id) throws IOException {
        Preconditions.checkNotNull(id);
        String index = ESModels.getIndex(modelClass);

        GetRequest request = new GetRequest()
                .index(index)
                .id(id);
        if (!getClient().isIgnoreIndexType()) {
            String type = ESModels.getType(modelClass);
            request.type(type);
        }
        try {
            GetResponse response = getClient().get(request, null);
            if (response.isExists() && !response.isSourceEmpty()) {
                return asModel(response, json, modelClass);
            }
        } catch (Throwable ex) {
            logRequestWhenFail(logger, request, ex);
            throw ex;
        }
        return null;
    }


    @Override
    public boolean removeById(String id) throws IOException {
        Preconditions.checkNotNull(id);
        String index = ESModels.getIndex(modelClass);

        DeleteRequest request = new DeleteRequest()
                .index(index)
                .id(id);
        if (!getClient().isIgnoreIndexType()) {
            String type = ESModels.getType(modelClass);
            request.type(type);
        }
        DeleteResponse response;
        try {
            response = client.delete(request, null);
            RestStatus rs = response.status();
            if (rs == OK) {
                return true;
            }
        } catch (Throwable ex) {
            logRequestWhenFail(logger, request, ex);
            throw ex;
        }
        return false;
    }

    /**
     * update doc
     */
    @Override
    public boolean updateById(String id, MODEL model) throws IOException {
        Preconditions.checkNotNull(model);
        String index = ESModels.getIndex(modelClass);
        UpdateRequest request = new UpdateRequest()
                .index(index)
                .id(id)
                .doc(json.toJson(model), XContentType.JSON)
                .fetchSource(false);

        if (!getClient().isIgnoreIndexType()) {
            String type = ESModels.getType(modelClass);
            request.type(type);
        }
        UpdateResponse response;
        try {
            response = client.update(request, null);
            RestStatus restStatus = response.status();
            if (restStatus == OK) {
                return true;
            }
        } catch (Throwable ex) {
            logRequestWhenFail(logger, request, ex);
            throw ex;
        }
        return false;
    }

    /**
     * 1)update doc
     * 2)get
     */
    @Override
    public MODEL replaceById(String id, MODEL model) throws IOException {
        Preconditions.checkNotNull(model);
        String index = ESModels.getIndex(modelClass);
        UpdateRequest request = new UpdateRequest()
                .index(index)
                .id(id)
                .doc(json.toJson(model), XContentType.JSON)
                .fetchSource(true);
        if (!getClient().isIgnoreIndexType()) {
            String type = ESModels.getType(modelClass);
            request.type(type);
        }
        UpdateResponse response;
        try {
            response = client.update(request, null);
            if (response.getGetResult().isExists() && !response.getGetResult().isSourceEmpty()) {
                return asModel(response.getGetResult(), json, modelClass);
            }
        } catch (Throwable ex) {
            logRequestWhenFail(logger, request, ex);
            throw ex;
        }
        return null;
    }

    /**
     * 1) doc_as_upsert
     * 2) get
     */
    @Override
    public MODEL merge(MODEL model) throws IOException {
        Preconditions.checkNotNull(model);
        String index = ESModels.getIndex(modelClass);
        String id = ESModels.getId(model);
        UpdateRequest request = new UpdateRequest()
                .index(index)
                .id(id)
                .doc(json.toJson(model), XContentType.JSON)
                .docAsUpsert(true)
                .fetchSource(true)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        if (!getClient().isIgnoreIndexType()) {
            String type = ESModels.getType(modelClass);
            request.type(type);
        }
        try {
            UpdateResponse response = client.update(request, null);
            if (response.getGetResult().isExists() && !response.getGetResult().isSourceEmpty()) {
                return asModel(response.getGetResult(), json, modelClass);
            }
        } catch (Throwable ex) {
            logRequestWhenFail(logger, request, ex);
            throw ex;
        }
        return null;
    }

    @Override
    public List<MODEL> getByIds(List<String> ids) throws IOException {
        Preconditions.checkNotNull(ids);
        final String index = ESModels.getIndex(modelClass);
        final String type = getClient().isIgnoreIndexType() ? null: ESModels.getType(modelClass);
        final MultiGetRequest request = new MultiGetRequest();
        Collects.forEach(ids, new Consumer<String>() {
            @Override
            public void accept(String id) {
                if (type == null) {
                    request.add(index, id);
                } else {
                    request.add(index, type, id);
                }
            }
        });

        final List<MODEL> list = new ArrayList<>();
        MultiGetResponse response;
        try {
            response = client.mget(request, null);
            Collects.forEach(response, new Consumer<MultiGetItemResponse>() {
                @Override
                public void accept(MultiGetItemResponse multiGetItemResponse) {
                    if (!multiGetItemResponse.isFailed()) {
                        if (multiGetItemResponse.getResponse().isExists() && !multiGetItemResponse.getResponse().isSourceEmpty()) {
                            list.add(asModel(multiGetItemResponse.getResponse(), json, modelClass));
                        }
                    }
                }
            });

        } catch (IOException ex) {
            logRequestWhenFail(logger, request, ex);
            throw ex;
        }

        return list;
    }

    @Override
    public void bulkMerge(List<MODEL> models) throws IOException {
        Preconditions.checkNotNull(models);

        final BulkRequest request = new BulkRequest();
        Collects.forEach(models, new Consumer<MODEL>() {
            @Override
            public void accept(MODEL model) {
                String index = ESModels.getIndex(model.getClass());
                String id = ESModels.getId(model);
                UpdateRequest mergeRequest = new UpdateRequest();
                mergeRequest.index(index)
                        .id(id)
                        .doc(json.toJson(model), XContentType.JSON)
                        .docAsUpsert(true)
                        .fetchSource(false);
                if (!getClient().isIgnoreIndexType()) {
                    String type = ESModels.getType(modelClass);
                    mergeRequest.type(type);
                }
                request.add(mergeRequest);
            }
        });

        BulkResponse bulkResponse;
        try {
            bulkResponse = client.bulk(request, null);
            if (bulkResponse.hasFailures()) {
                bulkResponse.buildFailureMessage();
            }
        } catch (IOException ex) {
            logRequestWhenFail(logger, request, ex);
            throw ex;
        }
    }

}
