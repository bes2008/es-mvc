package com.jn.esmvc.service;


import com.jn.esmvc.service.request.cat.CatClientWrapper;
import com.jn.esmvc.service.request.document.action.count.CountRequest;
import com.jn.esmvc.service.request.document.action.count.CountResponse;
import com.jn.esmvc.service.request.document.action.termvectors.TermVectorsRequest;
import com.jn.esmvc.service.request.document.action.termvectors.TermVectorsResponse;
import com.jn.esmvc.service.request.indices.IndicesClientWrapper;
import com.jn.langx.util.struct.Holder;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;

import java.io.IOException;

public abstract class ClientWrapper<CLIENT, OPTIONS> extends Holder<CLIENT> {

    private OPTIONS globalOptions;

    public void setGlobalOptions(OPTIONS options) {
        this.globalOptions = options;
    }

    public OPTIONS getGlobalOptions() {
        return globalOptions;
    }

    public abstract <WRAPPER> IndicesClientWrapper<WRAPPER, OPTIONS> indicesClient();

    public abstract <WRAPPER> CatClientWrapper<WRAPPER, OPTIONS> cat();

    /**
     * 服务端版本是否大于等于7
     * @return
     */
    private boolean serverVersionGE7 = false;
    /**
     * 服务端版本是否大于等于7
     * @return
     */
    public boolean isServerVersionGE7(){
        return this.serverVersionGE7;
    }

    public void setServerVersionGE7(boolean serverVersionGE7) {
        this.serverVersionGE7 = serverVersionGE7;
    }

    /**
     * 同步 index
     *
     * @param request
     * @param options
     * @return
     */
    public abstract IndexResponse index(IndexRequest request, OPTIONS options) throws IOException;

    /**
     * 异步 index
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void indexAsync(IndexRequest request, OPTIONS options, ActionListener<IndexResponse> listener);

    /**
     * 同步 delete
     *
     * @param request
     * @param options
     * @return
     */
    public abstract DeleteResponse delete(DeleteRequest request, OPTIONS options) throws IOException;

    /**
     * 异步 delete
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void deleteAsync(DeleteRequest request, OPTIONS options, ActionListener<DeleteResponse> listener);

    /**
     * 同步 update
     *
     * @param request
     * @param options
     * @return
     */
    public abstract UpdateResponse update(UpdateRequest request, OPTIONS options) throws IOException;

    /**
     * 异步 update
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void updateAsync(UpdateRequest request, OPTIONS options, ActionListener<UpdateResponse> listener);


    /**
     * 同步 get
     *
     * @param request
     * @param options
     * @return
     */
    public abstract GetResponse get(GetRequest request, OPTIONS options) throws IOException;

    /**
     * 异步 get
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void getAsync(GetRequest request, OPTIONS options, ActionListener<GetResponse> listener);

    /**
     * 同步 mget
     *
     * @param request
     * @param options
     * @return
     */
    public abstract MultiGetResponse mget(MultiGetRequest request, OPTIONS options) throws IOException;

    /**
     * 异步 mget
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void mgetAsync(MultiGetRequest request, OPTIONS options, ActionListener<MultiGetResponse> listener);


    /**
     * 同步 bulk
     *
     * @param request
     * @param options
     * @return
     */
    public abstract BulkResponse bulk(BulkRequest request, OPTIONS options) throws IOException;

    /**
     * 异步 bulk
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void bulkAsync(BulkRequest request, OPTIONS options, ActionListener<BulkResponse> listener);

    /**
     * 异步 search
     *
     * @param request
     * @param options
     */
    public abstract SearchResponse search(SearchRequest request, OPTIONS options) throws IOException;


    /**
     * 异步 search
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void searchAsync(SearchRequest request, OPTIONS options, ActionListener<SearchResponse> listener);


    /**
     * 异步 search scroll
     *
     * @param request
     * @param options
     */
    public abstract SearchResponse searchScroll(SearchScrollRequest request, OPTIONS options) throws IOException;


    /**
     * 异步 search scroll
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void searchScrollAsync(SearchScrollRequest request, OPTIONS options, ActionListener<SearchResponse> listener);

    /**
     * 异步 clear scroll
     *
     * @param request
     * @param options
     */
    public abstract ClearScrollResponse clearScroll(ClearScrollRequest request, OPTIONS options) throws IOException;


    /**
     * 异步 clear scroll
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void clearScrollAsync(ClearScrollRequest request, OPTIONS options, ActionListener<ClearScrollResponse> listener);


    /**
     * 异步 clear scroll
     *
     * @param request
     * @param options
     */
    public abstract MultiSearchResponse msearch(MultiSearchRequest request, OPTIONS options) throws IOException;


    /**
     * 异步 clear scroll
     *
     * @param request
     * @param options
     * @param listener
     */
    public abstract void msearch(MultiSearchRequest request, OPTIONS options, ActionListener<MultiSearchResponse> listener);


    public abstract TermVectorsResponse termVectors(TermVectorsRequest request, OPTIONS options) throws IOException;

    public abstract void termVectorsAsync(TermVectorsRequest request, OPTIONS options, ActionListener<TermVectorsResponse> listener);


    public abstract CountResponse count(CountRequest request, OPTIONS options) throws IOException;

    public abstract void countAsync(CountRequest request, OPTIONS options, ActionListener<CountResponse> listener);


}
