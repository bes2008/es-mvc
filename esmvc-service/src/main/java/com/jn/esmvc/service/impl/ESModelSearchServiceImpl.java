package com.jn.esmvc.service.impl;

import com.jn.esmvc.model.AbstractESModel;
import com.jn.esmvc.model.utils.ESModels;
import com.jn.esmvc.service.AbstractESModelService;
import com.jn.esmvc.service.ESModelSearchService;
import com.jn.esmvc.service.request.document.action.count.CountRequest;
import com.jn.esmvc.service.request.document.action.count.CountResponse;
import com.jn.esmvc.service.scroll.ScrollContext;
import com.jn.esmvc.service.scroll.ScrollContextCache;
import com.jn.esmvc.service.util.ESRequests;
import com.jn.langx.util.Emptys;
import com.jn.langx.util.Preconditions;
import com.jn.langx.util.collection.Collects;
import com.jn.langx.util.function.Consumer;
import com.jn.langx.util.pagination.PagingRequest;
import com.jn.langx.util.pagination.PagingResult;
import com.jn.langx.util.reflect.Reflects;
import org.elasticsearch.action.search.*;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static com.jn.esmvc.service.util.ESRequests.*;

public class ESModelSearchServiceImpl<MODEL extends AbstractESModel> extends AbstractESModelService<MODEL> implements ESModelSearchService<MODEL> {
    private static final Logger logger = LoggerFactory.getLogger(ESModelSearchServiceImpl.class);
    private ScrollContextCache searchScrollCache;
    private long scrollDuration = 60 * 1000;

    @Override
    public void setScrollCache(ScrollContextCache scrollContextCache) {
        this.searchScrollCache = scrollContextCache;
    }

    @Override
    public void setScrollDuration(long durationInMills) {
        this.scrollDuration = durationInMills;
    }

    @Override
    public long count(SearchSourceBuilder bodyBuilder) throws IOException {
        return count("_id", bodyBuilder);
    }

    @Override
    public long count(String countColumn, SearchSourceBuilder bodyBuilder) throws IOException {
        String index = ESModels.getIndex(modelClass);

        CountRequest request = new CountRequest();
        request.indices(index);
        if (!getClient().isIgnoreIndexType()) {
            String type = ESModels.getType(modelClass);
            request.types(type);
        }
        request.source(bodyBuilder);
        request.countColumn(countColumn);
        try {
            CountResponse response = getClient().count(request, null);
            return response.getCount();
        } catch (Throwable ex) {
            logRequestWhenFail(logger, request, ex);
        }

        return 0;
    }

    protected List<MODEL> extractSearchResults(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        List<MODEL> models = new LinkedList<>();
        long totalHits = ESRequests.getSearchTotalHits(searchHits);
        if (totalHits > 0) {
            for (SearchHit hit : searchHits.getHits()) {
                if (hit.hasSource()) {
                    models.add(asModel(hit, json, modelClass));
                }
            }
        }

        PagingRequest pr = ES_PAGING.get();
        if (pr != null) {
            PagingResult<MODEL> pagingResult = new PagingResult<MODEL>();
            pagingResult.setItems(models);
            pagingResult.setPageNo(pr.getPageNo());
            pagingResult.setPageSize(pr.getPageSize());
            pagingResult.setTotal(totalHits);
            pr.setResult(pagingResult);
        }

        return models;
    }

    protected List<MODEL> extractScrollResults(SearchResponse response, SearchSourceBuilder searchBodyBuilder, ScrollContext<MODEL> scrollContext) {
        List<MODEL> results = new LinkedList<>();
        results.addAll(scrollContext.getScrolledModels());
        SearchHits searchHits = response.getHits();
        long totalHits = ESRequests.getSearchTotalHits(searchHits);
        if (totalHits > 0) {
            List<MODEL> models = new LinkedList<>();
            for (SearchHit hit : searchHits.getHits()) {
                if (hit.hasSource()) {
                    models.add(asModel(hit, json, modelClass));
                }
            }

            int addToResultSize = searchBodyBuilder.size() - results.size();
            List<MODEL> remainResults = null;
            if (addToResultSize > 0) {
                if (models.size() > addToResultSize) {
                    results.addAll(models.subList(0, addToResultSize));
                    remainResults = models.subList(addToResultSize, models.size());
                } else {
                    results.addAll(models);
                }
            }
            String scrollId = response.getScrollId();
            if (remainResults != null && !remainResults.isEmpty()) {
                if (scrollId.equals(scrollContext.getScrollId())) {
                    scrollContext.setScrolledModels(remainResults);
                } else {
                    ScrollContext newScrollContext = new ScrollContext();
                    newScrollContext.setScrollId(response.getScrollId());
                    newScrollContext.setScroll(scrollContext.getScroll());
                    newScrollContext.setClientProxy(client);
                    scrollContext.setScrolledModels(remainResults);
                    searchScrollCache.put(searchBodyBuilder.query(), newScrollContext);
                }
            } else {
                searchScrollCache.clear(searchBodyBuilder.query());
            }
        } else {
            searchScrollCache.clear(searchBodyBuilder.query());
        }

        PagingRequest<MODEL> pr = ES_PAGING.get();
        if (pr != null) {
            PagingResult<MODEL> pagingResult = new PagingResult<MODEL>();
            pagingResult.setItems(results);
            pagingResult.setPageNo(pr.getPageNo());
            pagingResult.setPageSize(pr.getPageSize());
            pagingResult.setTotal(totalHits);
            pr.setResult(pagingResult);
        }
        return results;
    }


    protected List<MODEL> searchWithScroll(SearchSourceBuilder searchSourceBuilder, ScrollContext sc) throws IOException {
        SearchScrollRequest request = new SearchScrollRequest()
                .scroll(sc.getScroll())
                .scrollId(sc.getScrollId());
        try {
            SearchResponse response = client.searchScroll(request, null);
            return extractScrollResults(response, searchSourceBuilder, sc);
        } catch (Throwable ex) {
            logRequestWhenFail(logger, request, ex);
            throw ex;
        }
    }

    protected List<MODEL> search(SearchSourceBuilder searchBodyBuilder, QueryBuilder queryBuilder) throws IOException {
        Preconditions.checkNotNull(queryBuilder);

        boolean useFromSizePagation = false;
        if (searchBodyBuilder == null) {
            searchBodyBuilder = new SearchSourceBuilder();
        }
        if (searchBodyBuilder.size() < 0) {
            // index max result window is 10000
            searchBodyBuilder.size(10000);
        }
        if (searchBodyBuilder.size() == 0) {
            return Collects.emptyArrayList();
        }
        if (searchBodyBuilder.from() >= 0) {
            useFromSizePagation = true;

            PagingRequest<MODEL> pr = new PagingRequest<MODEL>();
            pr.setPageSize(searchBodyBuilder.size());
            int pageNo = searchBodyBuilder.from() / searchBodyBuilder.size() + 1;
            pr.setPageNo(pageNo);
            ES_PAGING.set(pr);
        }

        if (searchBodyBuilder.from() + searchBodyBuilder.size() > 10000) {
            Reflects.setDeclaredFieldValue(searchBodyBuilder, "from", -1, true, false);
            Reflects.setDeclaredFieldValue(searchBodyBuilder, "size", -1, true, false);
            useFromSizePagation = false;
        }

        ScrollContext<MODEL> scrollContext = (ScrollContext<MODEL>) searchScrollCache.get(queryBuilder);
        searchBodyBuilder.query(queryBuilder);

        if (useFromSizePagation) {
            String index = ESModels.getIndex(modelClass);

            SearchRequest request = new SearchRequest()
                    .indices(index)
                    .searchType(SearchType.QUERY_THEN_FETCH);
            if (!getClient().isIgnoreIndexType()) {
                String type = ESModels.getType(modelClass);
                request.types(type);
            }
            request.source(searchBodyBuilder);
            SearchResponse response = client.search(request, null);
            return extractSearchResults(response);
        } else {
            String index = ESModels.getIndex(modelClass);
            if (scrollContext == null) {
                // first search
                SearchRequest request = new SearchRequest()
                        .indices(index)
                        .searchType(SearchType.QUERY_THEN_FETCH);
                if (!getClient().isIgnoreIndexType()) {
                    String type = ESModels.getType(modelClass);
                    request.types(type);
                }
                scrollContext = new ScrollContext<>();
                scrollContext.setScroll(new Scroll(TimeValue.timeValueMillis(scrollDuration)));
                request.source(searchBodyBuilder);
                request.scroll(scrollContext.getScroll());
                SearchResponse response;
                try {
                    response = client.search(request, null);
                    return extractScrollResults(response, searchBodyBuilder, scrollContext);
                } catch (Throwable ex) {
                    logRequestWhenFail(logger, request, ex);
                }
                return Collects.emptyArrayList();

            } else {
                List<MODEL> cachedModels = scrollContext.getScrolledModels();
                if (Emptys.isEmpty(cachedModels)) {
                    return searchWithScroll(searchBodyBuilder, scrollContext);
                } else {
                    if (searchBodyBuilder.size() > cachedModels.size()) {
                        return searchWithScroll(searchBodyBuilder, scrollContext);
                    } else {
                        List<MODEL> results = cachedModels.subList(0, searchBodyBuilder.size());
                        scrollContext.setScrolledModels(cachedModels.subList(searchBodyBuilder.size(), cachedModels.size()));
                        return results;
                    }
                }

            }
        }
    }

    @Override
    public List<MODEL> search(SearchSourceBuilder bodyBuilder) throws IOException {
        return search(bodyBuilder, bodyBuilder.query());
    }


    @Override
    public List<MODEL> union(SearchSourceBuilder... bodyBuilders) throws IOException {
        MultiSearchRequest request = new MultiSearchRequest();
        String index = ESModels.getIndex(modelClass);
        String type = getClient().isIgnoreIndexType() ? null : ESModels.getType(modelClass);
        for (SearchSourceBuilder bodyBuilder : bodyBuilders) {
            SearchRequest r = new SearchRequest();
            r.indices(index).types(type);
            if (!getClient().isIgnoreIndexType()) {
                r.types(type);
            }
            r.source(bodyBuilder);
            request.add(r);
        }
        MultiSearchResponse multiSearchResponse = null;
        final Set<MODEL> list = new HashSet<>();

        try {
            multiSearchResponse = client.msearch(request, null);
            Collects.forEach(multiSearchResponse, new Consumer<MultiSearchResponse.Item>() {
                @Override
                public void accept(MultiSearchResponse.Item item) {
                    if (!item.isFailure()) {
                        SearchResponse response = item.getResponse();
                        SearchHits searchHits = response.getHits();
                        for (SearchHit hit : searchHits.getHits()) {
                            if (hit.hasSource()) {
                                list.add(asModel(hit, json, modelClass));
                            }
                        }
                    }
                }
            });
        } catch (Throwable ex) {
            logRequestWhenFail(logger, request, ex);
        }

        return new ArrayList<>(list);
    }

    @Override
    public List<MODEL> all(SearchSourceBuilder searchBodyBuilder) throws IOException {
        MatchAllQueryBuilder queryBuilder = new MatchAllQueryBuilder();
        return search(searchBodyBuilder, queryBuilder);
    }

    @Override
    public List<MODEL> none(SearchSourceBuilder bodyBuilder) throws IOException {
        return Collections.emptyList();
    }


    @Override
    public void clearScroll(String scrollId) {
        ESRequests.clearScroll(scrollId, client);
    }
}
