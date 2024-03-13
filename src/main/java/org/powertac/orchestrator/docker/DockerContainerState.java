package org.powertac.orchestrator.docker;

public enum DockerContainerState {
    UNKNOWN,    // state can't be determined
    CREATED,    // exists, but never started
    RUNNING,    // currently running
    RESTARTING, // currently restarting
    EXITED,     // exists, but not currently running
    PAUSED,     // processes suspended for indefinite time
    DEAD,       // non-functioning container
    NONE;       // container doesn't exist
}
