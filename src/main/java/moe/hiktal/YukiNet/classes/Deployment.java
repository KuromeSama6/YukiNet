package moe.hiktal.YukiNet.classes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import moe.hiktal.YukiNet.HttpUtil;
import moe.hiktal.YukiNet.ServerManager;

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
