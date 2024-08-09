package moe.hiktal.yukinet.command.impl;

import com.beust.jcommander.Parameter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.command.Command;
import moe.hiktal.yukinet.command.CommandHandler;
import moe.hiktal.yukinet.command.validation.PositiveIntegerValidator;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.service.Console;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@CommandHandler
public class RebootCommand extends Command<RebootCommand.Params> {
    private Timer timer;
    public RebootCommand() {
        super("reboot", "restart");
    }

    @Override
    protected void ExecuteInternal(Params params, List<String> args) throws IOException {
        if (args.isEmpty()) {
            Reject();
            return;
        }

        if (params.cancel) {
            if (timer != null) {
                timer.cancel();
                timer = null;
                GetLogger().info("Cancelled");
            } else GetLogger().info("Not shutting down");
            return;
        }

        if (timer != null || YukiNet.getServerManager().isShuttingDown()) {
            GetLogger().info("Already rebooting");
            return;
        }

        if (params.now || params.time <= 0) Shutdown();
        else {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        Shutdown();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, 1000L * params.time);
            GetLogger().info("Shutting down after %d seconds".formatted(params.time));

            if (params.message != null) {
                Server proxy = YukiNet.getServerManager().getProxy();
                if (proxy != null) proxy.SendInput("alert %s".formatted(params.message));
            }

        }

    }

    private void Shutdown() throws IOException {
        if (YukiNet.getServerManager().isShuttingDown()) return;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        Console.getInstance().setUserInterruptionWarned(true);
        YukiNet.getInstance().Reboot();
    }

    public static class Params {
        @Parameter(
                names = "-now",
                description = "Signals YukiNet to reboot immediately."
        )
        private boolean now;
        @Parameter(
                names = "-t",
                description = "Signals YukiNet to reboot after the specified amount of seconds.",
                validateWith = PositiveIntegerValidator.class
        )
        private int time = 60;
        @Parameter(
                names = "--cancel",
                description = "Cancels the current reboot attempt."
        )
        private boolean cancel;
        @Parameter(
                names = "-msg",
                description = "Broadcasts a message to all players before rebooting."
        )
        private String message;

    }
}
