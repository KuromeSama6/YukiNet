package moe.hiktal.yukinet.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import moe.hiktal.yukinet.enums.EStandardHttpStatus;

public class StandardHttpResponse {
    public EStandardHttpStatus status;
    public String message;
    public transient int statusCode = 200;
    public transient JsonObject data;
    public transient String rawContent;

    public StandardHttpResponse(int statusCode, EStandardHttpStatus status, String message) {
        this.status = status;
        this.message = message;
        this.statusCode = statusCode;

        data = new Gson().toJsonTree(this).getAsJsonObject();

        data.addProperty("status", status.toString().toLowerCase());
        data.addProperty("message", message);
    }

    public StandardHttpResponse(int statusCode, EStandardHttpStatus status, String message, JsonObject node) {
        this.status = status;
        this.message = message;
        this.statusCode = statusCode;

        data = node;

        data.addProperty("status", status.toString().toLowerCase());
        data.addProperty("message", message);
    }

    public StandardHttpResponse(EStandardHttpStatus status, String message) {
        this(200, status, message);
    }
    public StandardHttpResponse(EStandardHttpStatus status, String message, JsonObject node) {
        this(200, status, message, node);
    }

    public String Dump() {
        return rawContent == null ? data.toString() : rawContent;
    }

    public StandardHttpResponse SetRawContent(String message) {
        rawContent = message;
        return this;
    }

    //region Statics
    public static StandardHttpResponse notImplementedError() {
        return new StandardHttpResponse(501, EStandardHttpStatus.FALSE, "not implemented");
    }

    public static StandardHttpResponse badParameterError() {
        return new StandardHttpResponse(400, EStandardHttpStatus.RED, "bad parameters");
    }

    public static StandardHttpResponse serverError() {
        return new StandardHttpResponse(500, EStandardHttpStatus.RED, "server error");
    }

    public static StandardHttpResponse notFoundError() {
        return new StandardHttpResponse(404, EStandardHttpStatus.RED, "not found");
    }

    public static StandardHttpResponse genericError() {
        return new StandardHttpResponse(500, EStandardHttpStatus.RED, "generic error");
    }
    //endregion

    public static class SuccessfulResponse extends StandardHttpResponse {
        public SuccessfulResponse() {
            super(200, EStandardHttpStatus.GREEN, "ok");
        }

        public SuccessfulResponse(JsonObject data) {
            super(200, EStandardHttpStatus.GREEN, "ok", data);
        }
    }

}
