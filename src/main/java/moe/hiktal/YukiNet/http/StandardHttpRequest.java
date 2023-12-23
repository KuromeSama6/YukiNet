package moe.hiktal.YukiNet.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class StandardHttpRequest {
    public JsonNode body;
    public HashMap<String, String> urlParams;
    public HttpExchange exchange;

    public StandardHttpRequest(HttpExchange ext, String endpoint) {
        exchange = ext;

        // body
        try {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "UTF-8");
            BufferedReader br = new BufferedReader(isr);

            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }

            br.close();
            isr.close();

            ObjectMapper mapper = new ObjectMapper();
            body = mapper.readTree(requestBody.toString());
            if (body == null) body = mapper.createObjectNode();
            urlParams = AsyncHttpHandler.ExtractUrlParams(endpoint, ext.getRequestURI());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
