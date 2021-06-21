package com.jn.esmvc.service.rest.document.action.count;

import com.jn.esmvc.service.request.document.action.ResponseAdapter;
import com.jn.esmvc.service.request.document.action.count.CountResponse;

public class RestCountResponseAdapter implements ResponseAdapter<org.elasticsearch.client.core.CountResponse, CountResponse> {
    @Override
    public CountResponse apply(org.elasticsearch.client.core.CountResponse countResponse) {
        CountResponse.ShardStats shardStats = new CountResponse.ShardStats(countResponse.getSuccessfulShards(), countResponse.getTotalShards(), countResponse.getSkippedShards(), countResponse.getShardFailures());
        return new CountResponse(countResponse.getCount(), countResponse.isTerminatedEarly(), shardStats);
    }
}
