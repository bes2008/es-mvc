package com.jn.esmvc.service.rest.request.cat;

import com.jn.esmvc.service.request.cat.CatClientWrapper;
import com.jn.esmvc.service.request.cat.action.CatNodeAttrsRequest;
import com.jn.esmvc.service.request.cat.action.CatNodeAttrsResponse;
import com.jn.esmvc.service.request.cat.action.CatNodesRequest;
import com.jn.esmvc.service.request.cat.action.CatNodesResponse;
import com.jn.esmvc.service.rest.RestClientWrapper;
import com.jn.langx.util.net.http.HttpMethod;
import com.jn.langx.util.Objs;
import com.jn.langx.util.Strings;
import com.jn.langx.util.collection.Collects;
import com.jn.langx.util.function.Consumer;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.CheckedFunction;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RestCatClientWrapper implements CatClientWrapper<RestClientWrapper, RequestOptions> {
    private RestClientWrapper restClientWrapper;

    public RestCatClientWrapper(RestClientWrapper restClient) {
        this.restClientWrapper = restClient;
    }

    @Override
    public RestClientWrapper getClientWrapper() {
        return restClientWrapper;
    }

    private RestHighLevelClient getHighLevelClient() {
        return restClientWrapper.get();
    }

    private RestClient getLowLevelClient() {
        return getHighLevelClient().getLowLevelClient();
    }

    /**
     * GET /_cat/nodes
     *
     * @param requestOptions
     * @param request
     * @return
     */
    @Override
    public CatNodesResponse nodes(RequestOptions requestOptions, CatNodesRequest request) {
        RequestOptions options = restClientWrapper.mergeRequestOptions(requestOptions);
        if (request == null) {
            request = new CatNodesRequest();
        }
        CatNodesResponse catNodesResponse = getClientWrapper().performRequestAndParseEntity(request,
                new CheckedFunction<CatNodesRequest, Request, IOException>() {
                    @Override
                    public Request apply(CatNodesRequest request) throws IOException {
                        Request req = new Request(HttpMethod.GET.name(), "/_cat/nodes");

                        if (request.getDataUnit() != null) {
                            req.addParameter("bytes", request.getDataUnit().getStandardSymbol());
                        }
                        if (Strings.isNotEmpty(request.getFormat())) {
                            req.addParameter("format", request.getFormat());
                        }
                        req.addParameter("full_id", "" + request.isFullId());
                        if (Objs.isNotEmpty(request.getMetrics())) {
                            req.addParameter("h", Strings.join(",", request.getMetrics()));
                        }
                        if (request.getTimeUnit() != null) {
                            req.addParameter("time", request.getTimeUnit().getSimpleName());
                        }
                        return req;
                    }
                }, options,
                new CheckedFunction<XContentParser, CatNodesResponse, IOException>() {
                    @Override
                    public CatNodesResponse apply(XContentParser xContentParser) throws IOException {
                        List<Object> list = xContentParser.listOrderedMap();

                        CatNodesResponse response = new CatNodesResponse();
                        Collects.forEach(list, new Consumer<Object>() {
                            @Override
                            public void accept(Object o) {
                                if (o instanceof Map) {
                                    response.addNode((Map) o);
                                }
                            }
                        });
                        return response;
                    }
                },
                null
        );
        return catNodesResponse;
    }

    public CatNodeAttrsResponse nodeattrs(RequestOptions requestOptions, CatNodeAttrsRequest request) {
        RequestOptions options = restClientWrapper.mergeRequestOptions(requestOptions);
        if (request == null) {
            request = new CatNodeAttrsRequest();
        }
        CatNodeAttrsResponse catNodeAttrsResponse = getClientWrapper().performRequestAndParseEntity(request,
                new CheckedFunction<CatNodeAttrsRequest, Request, IOException>() {
                    @Override
                    public Request apply(CatNodeAttrsRequest request) throws IOException {
                        Request req = new Request(HttpMethod.GET.name(), "/_cat/nodeattrs");
                        if (Strings.isNotEmpty(request.getFormat())) {
                            req.addParameter("format", request.getFormat());
                        }
                        if (Objs.isNotEmpty(request.getMetrics())) {
                            req.addParameter("h", Strings.join(",", request.getMetrics()));
                        }
                        return req;
                    }
                }, options,
                new CheckedFunction<XContentParser, CatNodeAttrsResponse, IOException>() {
                    @Override
                    public CatNodeAttrsResponse apply(XContentParser xContentParser) throws IOException {
                        List<Object> list = xContentParser.listOrderedMap();

                        CatNodeAttrsResponse response = new CatNodeAttrsResponse();
                        Collects.forEach(list, new Consumer<Object>() {
                            @Override
                            public void accept(Object o) {
                                if (o instanceof Map) {
                                    response.addNodeAttr((Map) o);
                                }
                            }
                        });
                        return response;
                    }
                },
                null
        );
        return catNodeAttrsResponse;
    }
}
