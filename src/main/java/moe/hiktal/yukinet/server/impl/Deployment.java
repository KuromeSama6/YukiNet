package moe.hiktal.yukinet.server.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.util.HttpUtil;
import moe.hiktal.yukinet.server.ServerManager;

import java.util.ArrayList;
import java.util.List;

public class Deployment {
    public final String ip;
    public final int port;
    public List<Server> servers = new ArrayList<>();

    public Deployment(String ip, int port, JsonArray json) {
        this.ip = ip;
        this.port = port;

        for (JsonElement ele : json) {
            JsonObject node = ele.getAsJsonObject();
            Server server = new RemoteServer(this, node.get("id").getAsString(), node.get("port").getAsInt());
            servers.add(server);
            YukiNet.getServerManager().getDynamicServers().add(server);
        }

    }

    public String GetPrettyAddress() {
        return "%s:%s".formatted(ip, port);
    }

    public void SendStartClearance() {
        HttpUtil.HttpPostSync("http://%s/internal/start/clearance/copy".formatted(GetPrettyAddress()), "{}");
    }

}
