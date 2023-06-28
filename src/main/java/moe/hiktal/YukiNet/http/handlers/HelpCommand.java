package moe.hiktal.YukiNet.http.handlers;

import moe.hiktal.YukiNet.Main;
import moe.hiktal.YukiNet.http.AsyncHttpHandler;
import moe.hiktal.YukiNet.http.StandardHttpRequest;
import moe.hiktal.YukiNet.http.StandardHttpResponse;
import moe.icegame.coreutils.DevUtil;
import org.jetbrains.annotations.NotNull;

public class HelpCommand extends AsyncHttpHandler {
    @NotNull
    @Override
    public StandardHttpResponse Squawk(StandardHttpRequest req) {
        String lines = DevUtil.ReadResourceFile(Main.class, "texts/help.txt");
        return new StandardHttpResponse.SuccessfulResponse().SetRawContent(lines);
    }
}
