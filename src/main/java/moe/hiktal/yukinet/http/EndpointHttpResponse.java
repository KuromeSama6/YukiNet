package moe.hiktal.yukinet.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EndpointHttpResponse {
    public String originalContent;
    public int code;
    public JsonNode content;
    public boolean sent;
    public boolean suc;
    public String msg, msgTranslated;

    public EndpointHttpResponse() {}

    public EndpointHttpResponse(int code, String msg) {
        this.originalContent = msg;
        this.code = code;

        try {
            content = new ObjectMapper().readTree(msg);

            suc = !content.has("status") || content.get("status").textValue().equalsIgnoreCase("green");
            this.msg = content.get("message").textValue();
            msgTranslated = content.has("message_cn") ? content.get("message_cn").textValue() : msg;

        } catch (JsonProcessingException e) {
            content = null;
        }

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
