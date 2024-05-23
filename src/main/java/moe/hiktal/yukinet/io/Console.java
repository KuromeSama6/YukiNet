package moe.hiktal.yukinet.io;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import lombok.Getter;
import lombok.Setter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.command.Command;
import moe.hiktal.yukinet.command.impl.ContextCommand;
import moe.hiktal.yukinet.command.CommandHandler;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.util.Util;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

public class Console extends Thread {
    @Getter
    private static Console instance;
    @Getter
    private final List<Command> commands = new ArrayList<>();
    private final StringsCompleter completer;
    @Setter
    private boolean userInterruptionWarned;

    public Console() {
        super("ConsoleThread");
        instance = this;

        // register commands using reflections
        List<String> completionsEntries = new ArrayList<>();
        for (var clazz : new Reflections(YukiNet.class.getPackageName()).getTypesAnnotatedWith(CommandHandler.class)) {
            try {
                Command<?> cmd = (Command<?>)clazz.getDeclaredConstructor()
                        .newInstance();
                completionsEntries.add(cmd.getLabel());
                completionsEntries.addAll(cmd.getAliases());
                RegisterCommand(cmd);

            } catch (Exception e) {
                YukiNet.getLogger().error("Error loading command %s".formatted(clazz.getName()));
                e.printStackTrace();
            }
        }

        completer = new StringsCompleter(completionsEntries.toArray(new String[0]));
    }

    public void RegisterCommand(Command cmd) {
        commands.add(cmd);
    }

    public Command<?> GetCommand(String str) {
        return commands.stream()
                .filter(c -> c.Matches(str) || c.getAliases().contains(str))
                .findFirst().orElse(null);
    }



    @Override
    public void run() {
        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build()) {

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(new DefaultParser())
                    .completer(completer)
                    .variable(LineReader.HISTORY_FILE, ".yukinet-history")
                    .build();

            History history = reader.getHistory();

            YukiNet.getLogger().info("Now accepting input.");

            while (!interrupted()) {
                String input;

                // get the prompt
                StringBuilder prompt = new StringBuilder()
                        .append("YukiNet");
                {
                    ContextCommand ctx = Command.GetInstance(ContextCommand.class);
                    if (!ctx.getContext().isEmpty()) {
                        prompt.append(": ~");
                        prompt.append(Util.FormatList(ctx.getContext().stream()
                                .map(Server::getId)
                                .collect(Collectors.toList()), 5));
                    }
                }
                prompt.append(":$ ");

                try {
                    input = reader.readLine(prompt.toString());
                    history.add(input);

                    if (input.isBlank()) continue;
                    List<String> args = Arrays.asList(input.split(" "));
                    String label = args.get(0);
                    Command<?> cmd = GetCommand(label);

                    if (cmd != null) {
                        try {
                            Object parameter = cmd.CreateParameterObject();
                            List<String> finalArgs = args.subList(1, args.size());
                            JCommander jCommander = JCommander.newBuilder()
                                    .addObject(parameter)
                                    .build();
                            if (args.size() > 1) jCommander.parse(finalArgs.toArray(new String[0]));
                            cmd.Execute(parameter, finalArgs);

                        } catch (ParameterException e) {
                            YukiNet.getLogger().warn("Bad command: %s".formatted(e.getMessage()));

                        } catch (Exception e) {
                            YukiNet.getLogger().error("\nError while parsing command:");
                            e.printStackTrace();
                        }

                    } else YukiNet.getLogger().warn("command not found: %s".formatted(label));

                } catch (UserInterruptException | EndOfFileException e) {
                    if (!userInterruptionWarned) {
                        YukiNet.getLogger().warn("Please do use that to stop YukiNet. Use the 'shutdown' command instead.");
                        userInterruptionWarned = true;
                    }

                } catch (Exception e) {

                }
            }

            YukiNet.getLogger().info("Console thread ended.");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
