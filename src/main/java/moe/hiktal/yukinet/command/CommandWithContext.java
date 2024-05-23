package moe.hiktal.yukinet.command;

import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.command.impl.ContextCommand;
import moe.hiktal.yukinet.io.Console;
import moe.hiktal.yukinet.server.Server;

import java.util.List;

public abstract class CommandWithContext<T> extends Command<T>{
    public CommandWithContext(String label, String... aliases) {
        super(label, aliases);
    }

    @Override
    protected final void ExecuteInternal(T params, List<String> args) throws Exception {
        List<Server> context = Command.GetInstance(ContextCommand.class).getContext();
        if (context.isEmpty()) {
            YukiNet.getLogger().warn("Context is empty; %s requires context to execute.".formatted(label));
            return;
        }

        int count = 0;
        for (var server : context) {
            try {
               boolean res = ExecuteOnServer(server, params);
               if (res) ++count;
               YukiNet.getLogger().info("%s/%s - %s".formatted(
                       server.getGroupId(), server.getId(),
                       res ? "OK" : "FAILED"
               ));
            } catch (Exception e) {
                YukiNet.getLogger().warn("%s/%s - ERROR".formatted(
                        server.getGroupId(), server.getId()
                ));
                e.printStackTrace();
            }
        }

        YukiNet.getLogger().info("Execution on %d/%d servers successful.".formatted(count, context.size()));
    }

    protected abstract boolean ExecuteOnServer(Server server, T params);
}
