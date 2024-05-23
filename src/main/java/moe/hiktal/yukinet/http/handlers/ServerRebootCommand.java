package moe.hiktal.yukinet.http.handlers;

import moe.hiktal.yukinet.util.HttpUtil;
import moe.hiktal.yukinet.server.ServerManager;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.enums.EStandardHttpStatus;
import moe.hiktal.yukinet.http.AsyncHttpHandler;
import moe.hiktal.yukinet.http.StandardHttpRequest;
import moe.hiktal.yukinet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class ServerRebootCommand extends AsyncHttpHandler {
    public ServerRebootCommand(String endpoint) {
        super(endpoint);
    }

    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        if (!ServerManager.AllowingServerReboot()) return new StandardHttpResponse(EStandardHttpStatus.RED, "auto reboot not enabled; use /stop/<REGEX> instead.");

        String regex = req.urlParams.getOrDefault("regex", "^$");
        List<Server> servers = ServerManager.GetAllServers().stream()
                .filter(c -> c.getId().matches(regex))
                .filter(c -> c.getStatus().IsRunning())
                .filter(c -> !c.isStatic())
                .toList();


        try {
            for (Server server : servers) ServerManager.RestartServer(server);
        } catch (IOException e) {
            return new StandardHttpResponse(EStandardHttpStatus.FALSE, "IOException");
        }
        return new StandardHttpResponse.SuccessfulResponse(HttpUtil.GeneratedServerOperationResult(servers));
    }
}
