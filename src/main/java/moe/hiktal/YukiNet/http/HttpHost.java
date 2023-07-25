package moe.hiktal.YukiNet.http;

import com.sun.net.httpserver.HttpServer;
import moe.hiktal.YukiNet.Logger;
import moe.hiktal.YukiNet.Main;
import moe.hiktal.YukiNet.http.StandardHttpRequest;
import moe.hiktal.YukiNet.http.StandardHttpResponse;
import moe.hiktal.YukiNet.enums.EStandardHttpStatus;
//import moe.hiktal.YukiNet.http.handlers.*;
import moe.hiktal.YukiNet.http.handlers.*;
import moe.hiktal.YukiNet.http.handlers.internal.ClearanceRequestHandler;
import moe.hiktal.YukiNet.http.handlers.internal.ClearanceRequestReceiveHandler;
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
            Logger.Info(String.format("HTTP Server running on port %s", port));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private void RegisterLightweightHandlers() {

    }

}
