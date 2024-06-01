package moe.hiktal.yukinet.http.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.server.ServerManager;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.http.AsyncHttpHandler;
import moe.hiktal.yukinet.http.StandardHttpRequest;
import moe.hiktal.yukinet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RegexListCommand extends AsyncHttpHandler {
    public RegexListCommand(String endpoint) {
        super(endpoint);
    }

    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        String regex = req.urlParams.getOrDefault("regex", ".*");
        List<Server> servers = YukiNet.getServerManager().GetAllServers().stream()
                .filter(c -> c.getId().matches(regex))
                .toList();
        
        JsonObject node = new JsonObject();
        JsonArray arr = new JsonArray();

        for (Server server : servers) {
            JsonObject pth = new JsonObject();

            pth.addProperty("id", server.getId());
            pth.addProperty("status", server.getStatus().toString());
            pth.addProperty("groupId", server.getGroupId());
            pth.addProperty("port", server.getPort());
            pth.addProperty("static", server.isStatic());

            arr.add(pth);
        }

        node.add("servers", arr);
        return new StandardHttpResponse.SuccessfulResponse(node);
    }
}
