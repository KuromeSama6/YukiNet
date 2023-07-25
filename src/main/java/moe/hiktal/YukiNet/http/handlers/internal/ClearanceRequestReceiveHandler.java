package moe.hiktal.YukiNet.http.handlers.internal;

import com.fasterxml.jackson.databind.node.ArrayNode;
import moe.hiktal.YukiNet.Logger;
import moe.hiktal.YukiNet.Main;
import moe.hiktal.YukiNet.ServerManager;
import moe.hiktal.YukiNet.classes.Deployment;
import moe.hiktal.YukiNet.http.AsyncHttpHandler;
import moe.hiktal.YukiNet.http.StandardHttpRequest;
import moe.hiktal.YukiNet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ClearanceRequestReceiveHandler extends AsyncHttpHandler {
    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        Logger.Info("Clearance received, clear to start.");
        try {
            ServerManager.StartAllServers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StandardHttpResponse.SuccessfulResponse();
    }
}
