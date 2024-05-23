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

public class StoppedListCommand extends AsyncHttpHandler {

    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        List<Server> servers = ServerManager.stoppedServers;

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        ArrayNode arr = mapper.createArrayNode();

        for (Server server : servers) {
            ObjectNode pth = mapper.createObjectNode();

            pth.put("id", server.getId());
            pth.put("groupId", server.getGroupId());
            pth.put("port", server.getPort());
            pth.put("static", server.getStatus().toString());

            arr.add(pth);
        }

        node.set("servers", arr);
        return new StandardHttpResponse.SuccessfulResponse(node);
    }
}
