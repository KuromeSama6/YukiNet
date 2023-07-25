package moe.hiktal.YukiNet.classes;

import moe.hiktal.YukiNet.enums.EServerStatus;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.function.Consumer;

public abstract class Server {
    protected String groupId;
    protected int groupIndex;
    protected String id;
    protected boolean isStatic;
    protected volatile Process proc;
    protected Thread procListener;
    protected YamlConfiguration config;
    protected EServerStatus status;
    protected StopWatch sw = new StopWatch();
    protected File cwd;
    protected int port;
    protected int pid;
    protected Timer keepAliveTimer = new Timer();
    protected ProcessHandle procHandle;

    public abstract Server Clone();

    public abstract void SetCwd(File parent);
    public abstract void SetProxy(File parent);

    public EServerStatus getStatus() {return status;}

    public String getId() {return id;}

    public int getPid() {return proc == null ? -1 : (int)proc.pid();} //

    public String getGroupId() {return groupId;}

    public int getPort() {return port;}

    public boolean getIsStatic() {return isStatic;}

    public YamlConfiguration getConfig() {return config;}

    public String getHost() {
        return "127.0.0.1";
    }

    public File getWorkingDirectory() {return cwd;}

    public boolean IsRunning() {return status.IsRunning();}

    public abstract String getScreenSessionName();

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

}
