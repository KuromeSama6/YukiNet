package moe.hiktal.yukinet.http.handlers;

import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.http.AsyncHttpHandler;
import moe.hiktal.yukinet.http.StandardHttpRequest;
import moe.hiktal.yukinet.http.StandardHttpResponse;
import moe.hiktal.yukinet.util.FileUtil;
import org.jetbrains.annotations.NotNull;

public class HelpCommand extends AsyncHttpHandler {
    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        String lines = FileUtil.ReadResourceFile(YukiNet.class, "texts/help.txt");
        return new StandardHttpResponse.SuccessfulResponse().SetRawContent(lines);
    }
}
