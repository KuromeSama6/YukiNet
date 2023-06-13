package moe.hiktal.YukiNet.enums;

public enum EServerStatus {
    /**
     * Has a groupId and groupIndex set, and is awaiting running.
     */
    WAIT,
    /**
     * Up and running.
     */
    RUNNING,
    /**
     * Stopped.
     */
    STOPPED;

    public boolean IsRunning() {
        return this == RUNNING;
    }

}
