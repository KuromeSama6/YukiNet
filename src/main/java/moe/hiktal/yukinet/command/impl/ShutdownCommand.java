package moe.hiktal.yukinet.command.impl;

import com.beust.jcommander.Parameter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.command.Command;
import moe.hiktal.yukinet.command.CommandHandler;
import moe.hiktal.yukinet.command.validation.PositiveIntegerValidator;
import moe.hiktal.yukinet.io.Console;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.server.ServerManager;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@CommandHandler
public class ShutdownCommand extends Command<ShutdownCommand.Params> {
    private Timer shutdownTimer;
    public ShutdownCommand() {
        super("shutdown", "stop");
    }

    @Override
    protected void ExecuteInternal(Params params, List<String> args) throws IOException {
        if (args.isEmpty()) {
            Reject();
            return;
        }

        if (params.cancel) {
            if (shutdownTimer != null) {
                shutdownTimer.cancel();
                GetLogger().info("Cancelled");
            } else GetLogger().info("Not shutting down");
            return;
        }

        if (shutdownTimer != null || ServerManager.isShuttingDown()) {
            GetLogger().info("Already shutting down");
            return;
        }

        if (params.now || params.time <= 0) Shutdown();
        else {
            shutdownTimer = new Timer();
            shutdownTimer.schedule(new TimerTask() {
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
                Server proxy = ServerManager.proxy;
                if (proxy != null) proxy.SendInput("alert %s".formatted(params.message));
            }

        }

    }

    private void Shutdown() throws IOException {
        if (ServerManager.isShuttingDown()) return;
        Console.getInstance().setUserInterruptionWarned(true);
        ServerManager.Shutdown();
    }

    public static class Params {
        @Parameter(
                names = "-now",
                description = "Signals YukiNet to stop immediately."
        )
        private boolean now;
        @Parameter(
                names = "-t",
                description = "Signals YukiNet to stop after the specified amount of seconds.",
                validateWith = PositiveIntegerValidator.class
        )
        private int time = 60;
        @Parameter(
                names = "--cancel",
                description = "Cancels the current shutdown attempt."
        )
        private boolean cancel;
        @Parameter(
                names = "-msg",
                description = "Broadcasts a message to all players before shutting down."
        )
        private String message;

    }
}
