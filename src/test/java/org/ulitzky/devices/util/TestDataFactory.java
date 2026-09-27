package org.ulitzky.devices.util;

import org.ulitzky.devices.api.v1.resource.DeviceResource;
import org.ulitzky.devices.api.v1.resource.DeviceState;
import org.ulitzky.devices.model.Device;

import java.time.LocalDateTime;
import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Device validDevice() {
        Device device = new Device();
        device.setId(UUID.randomUUID());
        device.setName("Galaxy S24");
        device.setBrand("Samsung");
        device.setState(org.ulitzky.devices.model.enums.DeviceState.AVAILABLE);
        device.setDateCreated(LocalDateTime.now());
        return device;
    }

    public static DeviceResource validDeviceResource() {
        DeviceResource resource = validDeviceResourceForCreation();
        resource.setId(UUID.randomUUID().toString());
        resource.setDateCreated(LocalDateTime.now());
        return resource;
    }

    public static DeviceResource validDeviceResourceForCreation() {
        DeviceResource resource = new DeviceResource();
        resource.setName("Galaxy S24");
        resource.setBrand("Samsung");
        resource.setState(DeviceState.AVAILABLE);
        return resource;
    }

    public static DeviceResource inUseDeviceResourceForCreation() {
        DeviceResource resource = new DeviceResource();
        resource.setName("Galaxy S24");
        resource.setBrand("Samsung");
        resource.setState(DeviceState.IN_USE);
        return resource;
    }
}
