package moe.hiktal.yukinet.enums;

public enum EServerStatus {
    /**
     * Has a groupId and groupIndex set, and is awaiting running.
     */
    WAIT,
    /**
     * Has a screen session ready but is not running yet.
     */
    SCREEN_READY,
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

    public boolean IsAlive() {return IsRunning() || this == SCREEN_READY;}

}
