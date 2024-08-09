package moe.hiktal.yukinet.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class AsyncHttpHandler implements HttpHandler {
    public String endpoint;

    public AsyncHttpHandler(String endpoint) {
        this.endpoint = endpoint;
    }

    public AsyncHttpHandler() {
        endpoint = "";
    }

    @Override
    public void handle(HttpExchange exchange) {
        CompletableFuture.runAsync(() -> {
            StandardHttpResponse response;
            try {
                response = Squawk(new StandardHttpRequest(exchange, endpoint));

            } catch (Exception e) {
                response = StandardHttpResponse.serverError();
                e.printStackTrace();
            }


            try {
                String msg = response.Dump();
//                System.out.println(msg);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(response.statusCode, msg.getBytes(StandardCharsets.UTF_8).length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(msg.getBytes(StandardCharsets.UTF_8));
                outputStream.close();

            } catch (IOException e) {
                return;
            }

        });

    }

    /**
     * ASYNC/BLOCKING. Feeds in an StandardHttpRequest, and returns a StandardHttpResponse.
     * This is executed on a different thread.
     * @param req request.
     * @return response.
     */
    public abstract @NotNull StandardHttpResponse Squawk(StandardHttpRequest req) throws Exception;

    public static HashMap<String, String> ExtractUrlParams(String templateUrl, URI url) {
        HashMap<String, String> ret = new HashMap<>();

        List<String> keywords = Arrays.asList(templateUrl.split("/"));
        List<String> actualUrl = Arrays.asList(url.getPath().split("/"));

//        System.out.println(templateUrl);
//        System.out.println(keywords);
//        System.out.println(actualUrl);

        for (String kw : keywords) {
            if (actualUrl.size() < keywords.size()) break;
            String candidate = actualUrl.get(keywords.indexOf(kw));
            if (candidate != null && kw.length() > 1) {
                ret.put(kw.substring(1), candidate);
            }
        }

        return ret;
    }

}
