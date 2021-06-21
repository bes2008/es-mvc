package com.jn.esmvc.examples.controller;

import com.jn.esmvc.service.request.cat.action.CatNodesResponse;
import com.jn.esmvc.service.rest.RestClientWrapper;
import com.jn.esmvc.service.rest.request.cat.RestCatClientWrapper;
import com.jn.esmvc.service.tcp.TcpClientWrapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/_cat")
public class CatController {

    private TcpClientWrapper tcpClientWrapper;
    private RestClientWrapper restClientWrapper;


    @RequestMapping("/nodes")
    public CatNodesResponse nodes() {
        RestCatClientWrapper clientWrapper = restClientWrapper.cat();
        return clientWrapper.nodes(null, null);
    }
}
