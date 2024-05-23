package moe.hiktal.yukinet.command.impl;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import moe.hiktal.yukinet.command.Command;
import moe.hiktal.yukinet.command.CommandHandler;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.server.ServerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@CommandHandler
public class ContextCommand extends Command<ContextCommand.Params> {
    @Getter
    private final List<Server> context = new ArrayList<>();

    public ContextCommand() {
        super("context", "ctx");
    }

    @Override
    public void ExecuteInternal(Params params, List<String> args) {
        if (params.ls) {
            GetLogger().info("Servers (%d):".formatted(context.size()));
            for (var server : context) {
                GetLogger().info("[%s] %s/%s - %s".formatted(
                        server.getClass().getSimpleName(),
                        server.getGroupId(),
                        server.getId(),
                        server.getStatus()
                ));
            }
            return;
        }

        if (params.clr) {
            context.clear();
            return;
        }

        // select ccontexts
        for (String regex : params.ids) {
            context.addAll(
                    ServerManager.GetAllServers().stream()
                            .filter(c -> c.getId().matches(regex))
                            .filter(c -> !context.contains(c))
                            .toList()
            );
        }
    }

    public void PerformOperation(Consumer<Server> supplier) {
        context.forEach(supplier);
    }

    public static class Params {
        @Parameter(
                description = "A list of server IDs. Regex supported."
        )
        private List<String> ids = new ArrayList<>();
        @Parameter(
                names = "-l",
                description = "Displays the current context."
        )
        private boolean ls;
        @Parameter(
                names = {"-clr", "-clear"},
                description = "Clears the current context."
        )
        private boolean clr;
    }
}
