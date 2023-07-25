package moe.hiktal.YukiNet.classes;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class RemoteServer extends Server{
    private boolean alive;
    private final Deployment deployment;

    public RemoteServer(Deployment deployment, String id, int port) {
        this.id = id;
        this.deployment = deployment;
        this.port = port;
    }

    @Override
    public String getHost() {
        return deployment.ip;
    }

    @Override
    public Server Clone() {
        return null;
    }

    @Override
    public void SetCwd(File parent) {

    }

    @Override
    public void SetProxy(File parent) {

    }

    @Override
    public String getScreenSessionName() {
        return null;
    }

    @Override
    public YamlConfiguration GetInfoDump() {
        return null;
    }

    @Override
    public boolean IsAlive() {
        return alive;
    }

    @Override
    public void Stop() throws IOException {

    }

    @Override
    public void Interrupt(boolean force) throws IOException {

    }
}
