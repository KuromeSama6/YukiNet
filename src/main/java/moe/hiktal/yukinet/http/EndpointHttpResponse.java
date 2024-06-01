package moe.hiktal.yukinet.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class EndpointHttpResponse {
    public String originalContent;
    public int code;
    public JsonObject content;
    public boolean sent;
    public boolean suc;
    public String msg, msgTranslated;

    public EndpointHttpResponse() {}

    public EndpointHttpResponse(int code, String msg) {
        this.originalContent = msg;
        this.code = code;

        content = new Gson().fromJson(msg, JsonObject.class);

        suc = !content.has("status") || content.get("status").getAsString().equalsIgnoreCase("green");
        this.msg = content.get("message").getAsString();
        msgTranslated = content.has("message_cn") ? content.get("message_cn").getAsString() : msg;

    }

    public boolean Successful() {
        return sent && code == 200 && content != null && suc;
    }

    public static EndpointHttpResponse failedResponse() {
        return new EndpointHttpResponse() {
            public final boolean sent = false;
        };
    }

}
