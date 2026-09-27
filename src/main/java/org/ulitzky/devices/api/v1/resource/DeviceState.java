package org.ulitzky.devices.api.v1.resource;

public enum DeviceState {

    AVAILABLE("Available"),
    IN_USE("In-use"),
    INACTIVE("Inactive");

    private final String description;

    DeviceState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
