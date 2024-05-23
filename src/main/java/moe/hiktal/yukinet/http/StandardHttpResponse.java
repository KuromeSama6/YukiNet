package moe.hiktal.yukinet.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import moe.hiktal.yukinet.enums.EStandardHttpStatus;

public class StandardHttpResponse {
    public EStandardHttpStatus status;
    public String message;
    @JsonIgnore public int statusCode = 200;
    @JsonIgnore public ObjectNode data;
    @JsonIgnore public String rawContent;

    public StandardHttpResponse(int statusCode, EStandardHttpStatus status, String message) {
        this.status = status;
        this.message = message;
        this.statusCode = statusCode;

        ObjectMapper mapper = new ObjectMapper();
        data = mapper.valueToTree(this);

        data.put("status", status.toString().toLowerCase());
        data.put("message", message);
    }

    public StandardHttpResponse(int statusCode, EStandardHttpStatus status, String message, ObjectNode node) {
        this.status = status;
        this.message = message;
        this.statusCode = statusCode;

        data = node;

        data.put("status", status.toString().toLowerCase());
        data.put("message", message);
    }

    public StandardHttpResponse(EStandardHttpStatus status, String message) {
        this(200, status, message);
    }
    public StandardHttpResponse(EStandardHttpStatus status, String message, ObjectNode node) {
        this(200, status, message, node);
    }

    public String Dump() {
        try {
            return rawContent == null ? new ObjectMapper().writeValueAsString(data) : rawContent;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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

        public SuccessfulResponse(ObjectNode data) {
            super(200, EStandardHttpStatus.GREEN, "ok", data);
        }
    }

}
