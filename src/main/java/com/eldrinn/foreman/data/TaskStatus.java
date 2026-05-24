package com.eldrinn.foreman.data;

public enum TaskStatus {
    OPEN,
    IN_PROGRESS,
    DONE;

    public static TaskStatus fromNBT(String name) {
        try {
            return TaskStatus.valueOf(name);
        } catch (IllegalArgumentException e) {
            // Unknown status in save data — fall back to OPEN and log a warning.
            // This can happen after a mod update that renames enum values.
            org.apache.logging.log4j.LogManager.getLogger("foreman")
                .warn("Unknown TaskStatus '{}' in save data, defaulting to OPEN", name);
            return OPEN;
        }
    }
}
