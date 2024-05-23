package moe.hiktal.yukinet.command;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import moe.hiktal.yukinet.YukiNet;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public abstract class Command<T> {
    private static final Map<Class<? extends Command>, Command> instances = new HashMap<>();
    @Getter
    protected final String label;
    @Getter
    protected final List<String> aliases;
    public Command(String label, String... aliases) {
        this.label = label;
        this.aliases = Arrays.asList(aliases);

        instances.put(getClass(), this);
    }

    public final void Execute(Object params, List<String> args) throws Exception {
        ExecuteInternal((T)params, args);
    }
    public String GetHelpPage() {
        FileConfiguration commands = YukiNet.commands;
        if (!commands.contains(label)) return "(No help for %s)".formatted(label);

        ConfigurationSection cfg = commands.getConfigurationSection(label);
        StringBuilder ret = new StringBuilder();
        ret.append("\n");
        ret.append("%s - %s\n".formatted(label, cfg.getString("abstract")));
        ret.append("usage: %s\n".formatted(cfg.getString("usage")));
        ret.append(String.join("\n", cfg.getStringList("desc")));
        ret.append("\n");

        T param = CreateParameterObject();
        if (!(param instanceof NOPParameter)) {
            for (var field : param.getClass().getDeclaredFields()) {
                Parameter annotation = field.getAnnotation(Parameter.class);
                if (annotation.names() != null) {
                    ret.append(String.join(", ", annotation.names()));
                    ret.append("\n");
                    ret.append("    %s\n".formatted(annotation.description()));
                }
                ret.append("\n");
            }
        }

        return ret.toString();
    }

    public final void ShowHelpPage() {GetLogger().info(GetHelpPage());}
    protected final void Reject() {
        GetLogger().warn("Bad command - check usage with /help %s".formatted(label));
    }

    protected abstract void ExecuteInternal(T params, List<String> args) throws Exception;

    public final boolean Matches(String label) {
        return this.label.equalsIgnoreCase(label) || aliases.contains(label.toLowerCase());
    }

    public final T CreateParameterObject() {
        try {
            ParameterizedType type = (ParameterizedType)getClass().getGenericSuperclass();
            Type argumentType = type.getActualTypeArguments()[0];
            if (!(argumentType instanceof Class<?>))
                throw new IllegalArgumentException("Command type %s has invalid type arguments".formatted(getClass()));
            Class<?> clazz = (Class<?>)argumentType;
            return (T)clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected final Logger GetLogger() {
        return YukiNet.getLogger();
    }

    @NotNull
    public static <T extends Command> T GetInstance(Class<T> clazz) {
        var ret = instances.get(clazz);
        if (ret == null || !clazz.isAssignableFrom(ret.getClass()))
            throw new IllegalArgumentException("Class %s has no instances");
        return (T)ret;
    }
}
