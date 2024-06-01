package moe.hiktal.yukinet.command.impl;

import com.beust.jcommander.Parameter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.command.Command;
import moe.hiktal.yukinet.command.CommandHandler;
import moe.hiktal.yukinet.util.Util;

import java.util.List;

@CommandHandler
public class DebugGetPidCommand extends Command<DebugGetPidCommand.Params> {
    public DebugGetPidCommand() {
        super("debug-get-pid");
    }

    @Override
    protected void ExecuteInternal(Params params, List<String> args) throws Exception {
        if (params.port != 0) {
            int res = Util.GetPidByPort(params.port);
            YukiNet.getLogger().debug(res);
        }
    }

    public static class Params {
        @Parameter(
                names = "-p",
                description = "Find a process id that matches this port."
        )
        private int port;
    }
}
