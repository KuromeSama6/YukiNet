package moe.hiktal.YukiNet.http.handlers.internal;

import com.fasterxml.jackson.databind.node.ArrayNode;
import moe.hiktal.YukiNet.Logger;
import moe.hiktal.YukiNet.Main;
import moe.hiktal.YukiNet.ServerManager;
import moe.hiktal.YukiNet.classes.Deployment;
import moe.hiktal.YukiNet.classes.Server;
import moe.hiktal.YukiNet.http.AsyncHttpHandler;
import moe.hiktal.YukiNet.http.StandardHttpRequest;
import moe.hiktal.YukiNet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Array;

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
            Logger.Info("Received clearance from %s:%s (%s/%s).".formatted(ip, port, ServerManager.receivedRemote, Main.expectDeployments));

            if (ServerManager.receivedRemote == Main.expectDeployments) {
                Logger.Info("All remotes received. Starting servers...");
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
