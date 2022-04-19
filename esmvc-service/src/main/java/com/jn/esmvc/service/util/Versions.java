package com.jn.esmvc.service.util;

import com.jn.esmvc.service.ClientWrapper;
import com.jn.esmvc.service.request.cat.action.CatNodesResponse;
import com.jn.esmvc.service.request.cat.action.NodeRuntime;
import com.jn.langx.util.Emptys;
import com.jn.langx.util.Objs;
import com.jn.langx.util.Strings;
import com.jn.langx.util.collection.Pipeline;
import com.jn.langx.util.function.Function;
import org.elasticsearch.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Versions {
    private static final Logger logger = LoggerFactory.getLogger(Versions.class);

    public static List<String> getServerVersions(ClientWrapper wrapper) {
        List<String> nodesVersions = null;
        try {
            CatNodesResponse response = wrapper.cat().nodes(null, null);
            if (response == null) {
                return null;
            }
            nodesVersions = Pipeline.of(response.getNodes()).map(new Function<NodeRuntime, String>() {
                @Override
                public String apply(NodeRuntime nodeRuntime) {
                    return nodeRuntime.getVersion();
                }
            }).distinct().asList();
        } catch (Throwable ex) {
            logger.warn("Error occur when get servers version with the {}, error: {}", wrapper.getClass(), ex.getMessage(), ex);
        }

        if (Emptys.isNotEmpty(nodesVersions)) {
            if (nodesVersions.size() > 1) {
                logger.warn("has multiple version: {}", Strings.join(",", nodesVersions));
            } else {
                String version = nodesVersions.get(0);
                if (Version.CURRENT.toString().equalsIgnoreCase(version)) {
                    logger.info("======Client version: {}, Server version: {}======", Version.CURRENT, version);
                } else {
                    logger.warn("======Client version: {}, Server version: {}======", Version.CURRENT, version);
                }
            }
            return nodesVersions;
        }

        logger.warn("Can't find the server version");
        return null;
    }

    public static boolean checkVersion(ClientWrapper client) {
        try {
            List<String> versions = getServerVersions(client);
            if (Objs.length(versions) == 1) {
                String version = versions.get(0);
                client.setServerVersionGE7(checkServerVersionGE7(version));
                return true;
            }
        }catch (Throwable ex){
            client.setServerVersionGE7(checkServerVersionGE7(Version.CURRENT.toString()));
        }
        return false;
    }

    private static final Version VERSION_7_0_0 = Version.fromString("7.0.0");// Version.fromString("");

    public static boolean checkServerVersionGE7(String version) {
        Version v = Version.fromString(version);
        return v.compareTo(VERSION_7_0_0) >=0;
    }
}
