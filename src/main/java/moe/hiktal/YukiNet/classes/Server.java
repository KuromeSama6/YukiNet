package moe.hiktal.YukiNet.classes;

import moe.hiktal.YukiNet.Logger;
import moe.hiktal.YukiNet.enums.EServerStatus;

public class Server {
    private final String groupId;
    private final int groupIndex;
    private final String id;
    private Process proc;
    private EServerStatus status;
    private final int port;

    public Server(String groupId, int groupIndex, int port) {
        this.groupId = groupId;
        this.groupIndex = groupIndex;
        this.port = port;
        this.id = String.format("%s%s", groupId, groupIndex);

        Logger.Info(String.format("Server %s initialized on port %s", id, port));
    }

    public EServerStatus getStatus() {return status;}

    public String getId() {return id;}

    public String getGroupId() {return groupId;}

    public int getPort() {return port;}

    public boolean IsRunning() {return status.IsRunning();}

}
