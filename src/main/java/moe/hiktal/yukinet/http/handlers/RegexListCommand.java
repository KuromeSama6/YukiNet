package moe.hiktal.yukinet.http.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        List<Server> servers = ServerManager.GetAllServers().stream()
                .filter(c -> c.getId().matches(regex))
                .toList();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        ArrayNode arr = mapper.createArrayNode();

        for (Server server : servers) {
            ObjectNode pth = mapper.createObjectNode();

            pth.put("id", server.getId());
            pth.put("status", server.getStatus().toString());
            pth.put("groupId", server.getGroupId());
            pth.put("port", server.getPort());
            pth.put("static", server.isStatic());

            arr.add(pth);
        }

        node.set("servers", arr);
        return new StandardHttpResponse.SuccessfulResponse(node);
    }
}
