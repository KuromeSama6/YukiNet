package moe.hiktal.yukinet.server.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.util.HttpUtil;
import moe.hiktal.yukinet.server.ServerManager;

import java.util.ArrayList;
import java.util.List;

public class Deployment {
    public final String ip;
    public final int port;
    public List<Server> servers = new ArrayList<>();

    public Deployment(String ip, int port, ArrayNode json) {
        this.ip = ip;
        this.port = port;

        json.iterator().forEachRemaining(node -> {
            Server server = new RemoteServer(this, node.get("id").textValue(), node.get("port").intValue());
            servers.add(server);
            ServerManager.dynamicServers.add(server);
        });

    }

    public String GetPrettyAddress() {
        return "%s:%s".formatted(ip, port);
    }

    public void SendStartClearance() {
        HttpUtil.HttpPostSync("http://%s/internal/start/clearance/copy".formatted(GetPrettyAddress()), "{}");
    }

}
