package moe.hiktal.yukinet.command.impl;

import moe.hiktal.yukinet.command.Command;
import moe.hiktal.yukinet.command.CommandHandler;
import moe.hiktal.yukinet.command.NOPParameter;
import moe.hiktal.yukinet.service.Console;

import java.util.List;
import java.util.stream.Collectors;

@CommandHandler
public class HelpCommand extends Command<NOPParameter> {
    public HelpCommand() {
        super("help", "?", "man");
    }

    @Override
    protected void ExecuteInternal(NOPParameter params, List<String> args) {
        if (args.isEmpty()) {
          StringBuilder msg = new StringBuilder();
          msg.append("Available commands: ");
          msg.append(Console.getInstance().getCommands().stream()
                  .map(Command::getLabel)
                  .collect(Collectors.joining(", ")));
          GetLogger().info(msg.toString());

        } else {
            Command<?> cmd = Console.getInstance().GetCommand(args.get(0));
            if (cmd != null) cmd.ShowHelpPage();
            else GetLogger().warn("no such command: %s".formatted(args.get(0)));
        }
    }
}
