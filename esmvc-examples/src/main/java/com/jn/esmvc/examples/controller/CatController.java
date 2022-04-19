package com.jn.esmvc.examples.controller;

import com.jn.esmvc.service.request.cat.action.CatNodeAttrsResponse;
import com.jn.esmvc.service.request.cat.action.CatNodesResponse;
import com.jn.esmvc.service.rest.RestClientWrapper;
import com.jn.esmvc.service.rest.request.cat.RestCatClientWrapper;
import com.jn.esmvc.service.tcp.TcpClientWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/_cat")
public class CatController {

    private TcpClientWrapper tcpClientWrapper;
    private RestClientWrapper restClientWrapper;


    @GetMapping("/nodes")
    public CatNodesResponse nodes() {
        RestCatClientWrapper clientWrapper = restClientWrapper.cat();
        return clientWrapper.nodes(null, null);
    }

    @GetMapping("/nodeattrs")
    public CatNodeAttrsResponse nodeattrs() {
        RestCatClientWrapper clientWrapper = restClientWrapper.cat();
        return clientWrapper.nodeattrs(null, null);
    }

    /*
    @Autowired
    public void setTcpClientWrapper(TcpClientWrapper tcpClientWrapper) {
        this.tcpClientWrapper = tcpClientWrapper;
    }

     */

    @Autowired
    public void setRestClientWrapper(RestClientWrapper restClientWrapper) {
        this.restClientWrapper = restClientWrapper;
    }
}
