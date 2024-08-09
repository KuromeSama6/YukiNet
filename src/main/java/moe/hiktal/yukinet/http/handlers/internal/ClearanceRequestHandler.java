package moe.hiktal.yukinet.http.handlers.internal;

import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.server.Deployment;
import moe.hiktal.yukinet.http.AsyncHttpHandler;
import moe.hiktal.yukinet.http.StandardHttpRequest;
import moe.hiktal.yukinet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ClearanceRequestHandler extends AsyncHttpHandler {
    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        final String ip = req.body.get("ip").getAsString();
        final int port = req.body.get("port").getAsInt();
        Deployment deployment = new Deployment(ip, port, req.body.get("servers").getAsJsonArray());
        boolean suc = YukiNet.getServerManager().AddDeployment(deployment);

        if (suc) {
            YukiNet.getServerManager().setReceivedRemote(YukiNet.getServerManager().getReceivedRemote() + 1);
            YukiNet.getLogger().info("Received clearance from %s:%s (%s/%s).".formatted(ip, port, YukiNet.getServerManager().getReceivedRemote(), YukiNet.getInstance().getExpectDeployments()));

            if (YukiNet.getServerManager().getReceivedRemote() == YukiNet.getInstance().getExpectDeployments()) {
                YukiNet.getLogger().info("All remotes received. Starting servers...");
                try {
                    YukiNet.getServerManager().RewriteBungeecordConfig();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return suc ? new StandardHttpResponse.SuccessfulResponse() : StandardHttpResponse.genericError();
    }
}
