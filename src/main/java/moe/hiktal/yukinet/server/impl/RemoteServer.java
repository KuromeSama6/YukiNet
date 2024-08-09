package moe.hiktal.yukinet.server.impl;

import moe.hiktal.yukinet.server.Deployment;
import moe.hiktal.yukinet.server.Server;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class RemoteServer extends Server {
    private boolean alive;

    public RemoteServer(Deployment deployment, String id, int port) {
        this.id = id;
        this.deployment = deployment;
        this.port = port;
    }

    @Override
    public String GetHost() {
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
    public String GetScreenSessionName() {
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
