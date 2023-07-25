package moe.hiktal.YukiNet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import moe.hiktal.YukiNet.classes.Server;
import okhttp3.*;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class HttpUtil {
    public static StandardHttpResponse HttpPostSync(String url, String content) {
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
            if (res.body() != null) return new StandardHttpResponse(code, res.body().string());
            else return new StandardHttpResponse(code, "{}");

        } catch (Exception e) {
            Logger.Warning(String.format("Error on HTTP Request, url = %s", url));
            e.printStackTrace();
            return StandardHttpResponse.failedResponse();
        }

    }

    public static void HttpPost(String url, String body, Consumer<StandardHttpResponse> callback) {
        CompletableFuture.runAsync(() -> {
           callback.accept(HttpPostSync(url, body));
        });
    }

    public static ObjectNode GeneratedServerOperationResult(Iterable<Server> servers) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode ret = mapper.createObjectNode();
        ArrayNode arr = mapper.createArrayNode();

        for (Server server : servers) arr.add(server.getId());

        ret.set("affected", arr);
        ret.put("count", arr.size());

        return ret;
    }

}
