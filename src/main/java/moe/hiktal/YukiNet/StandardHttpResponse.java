package moe.hiktal.YukiNet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StandardHttpResponse {
    public String originalContent;
    public int code;
    public JsonNode content;
    public boolean sent;
    public boolean suc;
    public String msg, msgTranslated;

    public StandardHttpResponse() {}

    public StandardHttpResponse(int code, String msg) {
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

    public static StandardHttpResponse failedResponse() {
        return new StandardHttpResponse() {
            public final boolean sent = false;
        };
    }

}
