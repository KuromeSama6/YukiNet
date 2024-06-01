package moe.hiktal.yukinet.server;

import lombok.Getter;
import lombok.Setter;
import moe.hiktal.yukinet.enums.EServerStatus;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.function.Consumer;

public abstract class Server {
    @Getter
    protected String groupId;
    protected int groupIndex;
    @Getter
    protected String id;
    @Getter
    protected boolean isStatic;
    protected volatile Process proc;
    protected Thread procListener;
    @Getter
    protected YamlConfiguration config;
    @Getter
    protected EServerStatus status = EServerStatus.WAIT;
    protected StopWatch sw = new StopWatch();
    protected File cwd;
    @Getter
    protected int port;
    @Getter
    protected int pid;
    protected Timer keepAliveTimer = new Timer();
    protected ProcessHandle procHandle;

    public abstract Server Clone();

    public abstract void SetCwd(File parent);
    public abstract void SetProxy(File parent);

    public boolean IsRunning() {return status.IsRunning();}

    public abstract String GetHost();
    public abstract String GetScreenSessionName();

    public abstract YamlConfiguration GetInfoDump();

    public void Start() throws IOException {Start(null);}

    public void Start(Consumer<Boolean> callback) throws IOException {
        throw new IllegalStateException("Call to Start() not permitted on non-local server");
    }

    protected void StartListener(Consumer<Boolean> callback) throws IOException {
        throw new IllegalStateException("Call to Start() not permitted on non-local server");
    }

    public abstract boolean IsAlive();

    public abstract void Stop() throws IOException;

    public void Interrupt() throws IOException {Interrupt(false);}

    public abstract void Interrupt(boolean force) throws IOException;

    public void SendInput(String input) throws IOException {
        if (status != EServerStatus.RUNNING) return;
        Runtime.getRuntime().exec(new String[] {
                "screen", "-S", GetScreenSessionName(), "-X", "stuff", "%s\n".formatted(input)
        });
    }

}
