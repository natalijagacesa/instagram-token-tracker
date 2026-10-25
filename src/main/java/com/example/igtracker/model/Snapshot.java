package com.example.igtracker.model;

public class Snapshot {
    private long id;
    private String snapshotName;
    private String snapshotType;
    private String usernamesText;
    private String createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public void setSnapshotName(String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public String getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public String getUsernamesText() {
        return usernamesText;
    }

    public void setUsernamesText(String usernamesText) {
        this.usernamesText = usernamesText;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return snapshotName + " [" + snapshotType + "] - " + createdAt;
    }
}
