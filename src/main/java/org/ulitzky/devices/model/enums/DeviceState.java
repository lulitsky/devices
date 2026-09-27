package org.ulitzky.devices.model.enums;

public enum DeviceState {

    AVAILABLE(true),
    IN_USE(false),
    INACTIVE(true);

    private final boolean validForDeletionOrUpdate;

    DeviceState(boolean validForDeletionOrUpdate) {
        this.validForDeletionOrUpdate = validForDeletionOrUpdate;
    }

    public boolean isValidForDeletionOrUpdate() {
        return validForDeletionOrUpdate;
    }
}
