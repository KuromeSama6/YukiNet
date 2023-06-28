package moe.hiktal.YukiNet.http.handlers;

import moe.hiktal.YukiNet.HttpUtil;
import moe.hiktal.YukiNet.ServerManager;
import moe.hiktal.YukiNet.classes.Server;
import moe.hiktal.YukiNet.enums.EStandardHttpStatus;
import moe.hiktal.YukiNet.http.AsyncHttpHandler;
import moe.hiktal.YukiNet.http.StandardHttpRequest;
import moe.hiktal.YukiNet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class ServerStopCommand extends AsyncHttpHandler {
    public ServerStopCommand(String endpoint) {
        super(endpoint);
    }

    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        String regex = req.urlParams.getOrDefault("regex", "^$");
        List<Server> servers = ServerManager.GetAllServers().stream()
                .filter(c -> c.getId().matches(regex))
                .filter(c -> !c.getIsStatic())
                .toList();

        try {
            for (Server server : servers) server.Stop();
        } catch (IOException e) {
            return new StandardHttpResponse(EStandardHttpStatus.FALSE, "IOException");
        }
        return new StandardHttpResponse.SuccessfulResponse(HttpUtil.GeneratedServerOperationResult(servers));
    }
}
