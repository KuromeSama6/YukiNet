package moe.hiktal.yukinet.http.handlers.internal;

import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.server.ServerManager;
import moe.hiktal.yukinet.http.AsyncHttpHandler;
import moe.hiktal.yukinet.http.StandardHttpRequest;
import moe.hiktal.yukinet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ClearanceRequestReceiveHandler extends AsyncHttpHandler {
    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        YukiNet.getLogger().info("Clearance received, clear to start.");
        try {
            ServerManager.StartAllServers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StandardHttpResponse.SuccessfulResponse();
    }
}
