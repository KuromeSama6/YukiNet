package moe.hiktal.yukinet.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.http.EndpointHttpResponse;
import moe.hiktal.yukinet.server.Server;
import okhttp3.*;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class HttpUtil {
    public static EndpointHttpResponse HttpPostSync(String url, String content) {
        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();

        //System.out.println(String.format("requesting to %s", url));

        RequestBody body = RequestBody.create(content, mediaType);
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response res = client.newCall(req).execute()) {
            int code = res.code();
            if (res.body() != null) return new EndpointHttpResponse(code, res.body().string());
            else return new EndpointHttpResponse(code, "{}");

        } catch (Exception e) {
            YukiNet.getLogger().warn(String.format("Error on HTTP Request, url = %s", url));
            e.printStackTrace();
            return EndpointHttpResponse.failedResponse();
        }

    }

    public static void HttpPost(String url, String body, Consumer<EndpointHttpResponse> callback) {
        CompletableFuture.runAsync(() -> {
           callback.accept(HttpPostSync(url, body));
        });
    }

    public static JsonObject GeneratedServerOperationResult(Iterable<Server> servers) {
        JsonObject ret = new JsonObject();
        JsonArray arr = new JsonArray();

        for (Server server : servers) arr.add(server.getId());

        ret.add("affected", arr);
        ret.addProperty("count", arr.size());

        return ret;
    }

}
