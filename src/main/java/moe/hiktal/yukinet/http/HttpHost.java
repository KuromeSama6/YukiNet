package moe.hiktal.yukinet.http;

import com.sun.net.httpserver.HttpServer;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.enums.EStandardHttpStatus;
//import moe.hiktal.YukiNet.http.handlers.*;
import moe.hiktal.yukinet.http.handlers.*;
import moe.hiktal.yukinet.http.handlers.internal.ClearanceRequestHandler;
import moe.hiktal.yukinet.http.handlers.internal.ClearanceRequestReceiveHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpHost {
    public static HttpHost i;

    public HttpServer server;

    public HttpHost(int port) {
        try {
            server = HttpServer.create();

            server.createContext("/", new AsyncHttpHandler() {
                @NotNull
                @Override
                public StandardHttpResponse Squawk(StandardHttpRequest req) {
                    return new StandardHttpResponse(404, EStandardHttpStatus.FALSE, "404 Not Found");
                }
            });

            server.createContext("/help", new HelpCommand());

            server.createContext("/list", new RegexListCommand("/a/:regex"));
            server.createContext("/liststopped", new StoppedListCommand());

            server.createContext("/stop", new ServerStopCommand("/a/:regex"));
            server.createContext("/reboot", new ServerRebootCommand("/a/:regex"));
            server.createContext("/start", new ServerStartCommand("/a/:regex"));

            server.createContext("/internal/start/clearance", new ClearanceRequestHandler());
            server.createContext("/internal/start/clearance/copy", new ClearanceRequestReceiveHandler());

            RegisterLightweightHandlers();
            server.bind(new InetSocketAddress(port), 0);
            server.start();
            YukiNet.getLogger().info(String.format("HTTP Server running on port %s", port));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private void RegisterLightweightHandlers() {

    }

}
