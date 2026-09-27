package org.ulitzky.devices.api.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.ulitzky.devices.api.v1.resource.DeviceResource;
import org.ulitzky.devices.exception.DeviceNotFoundException;
import org.ulitzky.devices.exception.DeviceNotValidForRequestedChangeException;
import org.ulitzky.devices.exception.InvalidDeviceStateException;
import org.ulitzky.devices.model.Device;
import org.ulitzky.devices.service.DeviceService;
import org.ulitzky.devices.service.mapper.DeviceMapper;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/v1/device")
@Tag(name = "Device", description = "Device management operations")
public class DeviceController {

    private final DeviceService deviceService;

    private final DeviceMapper deviceMapper;

    @Autowired
    DeviceController(final DeviceService deviceService, final DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceService = deviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a device")
    public DeviceResource create(@Valid @RequestBody DeviceResource deviceData)
    {
        Device created = deviceService.create(deviceMapper.mapResourceToEntity(deviceData));
        return deviceMapper.mapEntityToResource(created);
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Find a device by id")
    public DeviceResource findById(@Parameter(description = "Device id") @PathVariable final UUID deviceId) throws DeviceNotFoundException {
        Device device= deviceService.findById(deviceId);
        return deviceMapper.mapEntityToResource(device);
    }

    @GetMapping
    @Operation(summary = "List devices, optionally filtered by brand or state")
    public List<DeviceResource> findAllBy(@Parameter(description = "Filter by brand") @RequestParam(required = false) String brand,
                                           @Parameter(description = "Filter by state") @RequestParam(required = false) String state) throws InvalidDeviceStateException {
        List<Device> devices = deviceService.findAllBy(brand, state);
        return deviceMapper.mapEntityListToResourceList(devices);
    }

    @PutMapping("/{deviceId}")
    @Operation(summary = "Fully update a device")
    public DeviceResource update(@Valid @RequestBody DeviceResource updatedDeviceData,
                                  @Parameter(description = "Device id") @PathVariable final UUID deviceId) throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException {
        Device updated = deviceService.update(deviceMapper.mapResourceToEntity(updatedDeviceData), deviceId);
        return deviceMapper.mapEntityToResource(updated);
    }

    @PatchMapping("/{deviceId}")
    @Operation(summary = "Partially update a device")
    public DeviceResource patch(@Parameter(description = "Device id") @PathVariable final UUID deviceId,
                                @Parameter(description = "New name") @RequestParam(required = false) String name,
                                @Parameter(description = "New brand") @RequestParam(required = false) String brand,
                                @Parameter(description = "New state") @RequestParam(required = false) String state) throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException, InvalidDeviceStateException {
        Device updated = deviceService.patch(deviceId, name, brand, state);
        return deviceMapper.mapEntityToResource(updated);
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Delete a device")
    public void delete(@Parameter(description = "Device id") @PathVariable final UUID deviceId) throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException {
        deviceService.delete(deviceId);
    }

}
