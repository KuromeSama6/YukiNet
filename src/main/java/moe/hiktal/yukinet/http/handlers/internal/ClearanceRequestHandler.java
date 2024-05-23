package moe.hiktal.yukinet.http.handlers.internal;

import com.fasterxml.jackson.databind.node.ArrayNode;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.server.ServerManager;
import moe.hiktal.yukinet.server.impl.Deployment;
import moe.hiktal.yukinet.http.AsyncHttpHandler;
import moe.hiktal.yukinet.http.StandardHttpRequest;
import moe.hiktal.yukinet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ClearanceRequestHandler extends AsyncHttpHandler {
    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        final String ip = req.body.get("ip").textValue();
        final int port = req.body.get("port").intValue();
        Deployment deployment = new Deployment(ip, port, (ArrayNode)req.body.get("servers"));
        boolean suc = ServerManager.AddDeployment(deployment);

        if (suc) {
            ++ServerManager.receivedRemote;
            YukiNet.getLogger().info("Received clearance from %s:%s (%s/%s).".formatted(ip, port, ServerManager.receivedRemote, YukiNet.expectDeployments));

            if (ServerManager.receivedRemote == YukiNet.expectDeployments) {
                YukiNet.getLogger().info("All remotes received. Starting servers...");
                try {
                    ServerManager.RewriteBungeecordConfig();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return suc ? new StandardHttpResponse.SuccessfulResponse() : StandardHttpResponse.genericError();
    }
}
