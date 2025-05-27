package com.dockpilot.model;

import lombok.Data;

@Data
public class VolumeMapping {
    private String hostPath;
    private String containerPath;
    private Boolean readOnly;

    public VolumeMapping(String hostPath, String containerPath, Boolean readOnly) {
        this.hostPath = hostPath;
        this.containerPath = containerPath;
        this.readOnly = readOnly;
    }


}