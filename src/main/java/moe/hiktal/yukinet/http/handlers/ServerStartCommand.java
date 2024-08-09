package moe.hiktal.yukinet.http.handlers;

import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.util.HttpUtil;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.enums.EStandardHttpStatus;
import moe.hiktal.yukinet.http.AsyncHttpHandler;
import moe.hiktal.yukinet.http.StandardHttpRequest;
import moe.hiktal.yukinet.http.StandardHttpResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class ServerStartCommand extends AsyncHttpHandler {
    public ServerStartCommand(String endpoint) {
        super(endpoint);
    }

    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        String regex = req.urlParams.getOrDefault("regex", "^$");
        List<Server> servers = YukiNet.getServerManager().GetAllLocalServers().stream()
                .filter(c -> c.getId().matches(regex))
                .filter(YukiNet.getServerManager().getStoppedServers()::contains)
                .toList();


        try {
            for (Server server : servers) server.Start();
        } catch (IOException e) {
            return new StandardHttpResponse(EStandardHttpStatus.FALSE, "IOException");
        }
        return new StandardHttpResponse.SuccessfulResponse(HttpUtil.GeneratedServerOperationResult(servers));
    }
}
