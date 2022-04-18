package com.jn.esmvc.service.util;

import com.jn.easyjson.core.JSON;
import com.jn.esmvc.model.AbstractESModel;
import com.jn.esmvc.service.ClientWrapper;
import com.jn.langx.util.Emptys;
import com.jn.langx.util.Preconditions;
import com.jn.langx.util.pagination.PagingRequest;
import com.jn.langx.util.reflect.Reflects;
import com.jn.langx.util.reflect.type.Types;
import com.jn.langx.util.struct.ThreadLocalHolder;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ESRequests {
    public static final ThreadLocalHolder<PagingRequest> ES_PAGING = new ThreadLocalHolder<PagingRequest>();
    private static final Logger logger = LoggerFactory.getLogger(ESRequests.class);

    public static void logRequestWhenFail(Logger logger, ActionRequest request, Throwable ex) {
        logger.error("Error occur when execute elasticsearch restful request {}", request, ex);
    }

    public static void clearScroll(String scrollId, ClientWrapper<?, ?> clientProxy) {
        Preconditions.checkNotNull(scrollId);
        ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(scrollId);
        try {
            clientProxy.clearScroll(request, null);
        } catch (Exception ex) {
            logRequestWhenFail(logger, request, ex);
        }
    }

    public static <MODEL extends AbstractESModel> MODEL asModel(GetResult result, JSON json, Class<MODEL> modelClass) {
        if (!result.isSourceEmpty()) {
            MODEL model = asModel(result.sourceAsString(), json, modelClass);
            model.set_id(result.getId());
            return model;
        }
        return null;
    }

    public static <MODEL extends AbstractESModel> MODEL asModel(GetResponse response, JSON json, Class<MODEL> modelClass) {
        if (response.isExists() && !response.isSourceEmpty()) {
            MODEL model = asModel(response.getSourceAsString(), json, modelClass);
            model.set_id(response.getId());
            return model;
        }
        return null;
    }

    public static <MODEL extends AbstractESModel> MODEL asModel(String source, JSON json, Class<MODEL> modelClass) {
        return json.fromJson(source, modelClass);
    }

    public static <MODEL extends AbstractESModel> MODEL asModel(SearchHit hit, JSON json, Class<MODEL> modelClass) {
        if (hit.hasSource()) {
            MODEL model = asModel(hit.getSourceAsString(), json, modelClass);
            model.set_id(hit.getId());
            Map<String, HighlightField> highlightFieldsMap = hit.getHighlightFields();
            if (highlightFieldsMap != null && !highlightFieldsMap.isEmpty()) {
                final Map<String, com.jn.esmvc.model.HighlightField> highlightFieldMap = new HashMap<String, com.jn.esmvc.model.HighlightField>(highlightFieldsMap.size());

                highlightFieldsMap.forEach((key, highlightFields) -> {
                    com.jn.esmvc.model.HighlightField highlightField = new com.jn.esmvc.model.HighlightField(highlightFields.name());
                    Text[] fragments = highlightFields.fragments();
                    for (Text fragment : fragments) {
                        if (fragment.hasString()) {
                            highlightField.addFragment(fragment.string());
                        }
                    }
                    highlightFieldMap.put(key, highlightField);
                });
                model.setHighlightFieldMap(highlightFieldMap);
            }
            return model;
        }
        return null;
    }

    private static final Method searchHits_getTotalHits = Reflects.getDeclaredMethod(SearchHits.class, "getTotalHits");

    public static long getSearchTotalHits(SearchHits searchHits) {
        // es 6.x , 7.x SearchHits#getTotalHits() 返回值发生变化
        Object obj = Reflects.invoke(searchHits_getTotalHits, searchHits, Emptys.EMPTY_OBJECTS, true, true);
        if (obj == null) {
            return searchHits.getHits().length;
        }
        if (obj instanceof Long || Types.isPrimitive(obj.getClass())) {
            return (long) obj;
        }
        return Reflects.getDeclaredFieldValue(obj, "value", true, true);
    }
}
