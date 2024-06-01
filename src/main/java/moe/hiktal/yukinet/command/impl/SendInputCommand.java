package moe.hiktal.yukinet.command.impl;

import moe.hiktal.yukinet.command.CommandHandler;
import moe.hiktal.yukinet.command.CommandWithContext;
import moe.hiktal.yukinet.command.NOPParameter;
import moe.hiktal.yukinet.enums.EServerStatus;
import moe.hiktal.yukinet.server.Server;

import java.io.IOException;
import java.util.List;

@CommandHandler
public class SendInputCommand extends CommandWithContext<NOPParameter> {
    public SendInputCommand() {
        super("sendinput", "execute", "do");
    }

    @Override
    protected boolean ExecuteOnServer(Server server, NOPParameter params, List<String> args) {
        if (server.getStatus() != EServerStatus.RUNNING) return false;

        try {
            server.SendInput(String.join(" ", args));
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
