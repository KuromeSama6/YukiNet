package moe.hiktal.yukinet.http.handlers.internal;

import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.http.AsyncHttpHandler;
import moe.hiktal.yukinet.http.StandardHttpRequest;
import moe.hiktal.yukinet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ServiceShutdownHandler extends AsyncHttpHandler {
    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) throws IOException {
        boolean reboot = req.body.get("reboot").getAsBoolean();
        YukiNet.getLogger().info("Received service shutdown notification from master (reboot = %s)".formatted(reboot));

        CompletableFuture.runAsync(() -> {
            try {
                if (reboot) YukiNet.getInstance().Reboot();
                else YukiNet.getInstance().Shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return new StandardHttpResponse.SuccessfulResponse();
    }
}
