package org.ulitzky.devices.api.v1.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ulitzky.devices.api.v1.resource.DeviceResource;
import org.ulitzky.devices.exception.DeviceNotFoundException;
import org.ulitzky.devices.exception.DeviceNotValidForRequestedChangeException;
import org.ulitzky.devices.exception.InvalidDeviceStateException;
import org.ulitzky.devices.model.Device;
import org.ulitzky.devices.service.DeviceService;
import org.ulitzky.devices.service.mapper.DeviceMapperImpl;
import org.ulitzky.devices.util.TestDataFactory;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Mock
    private DeviceService deviceService;

    private DeviceController deviceController;


    @BeforeEach
    void init() {
        deviceController = new DeviceController(deviceService, new DeviceMapperImpl());

    }

    @Test
    void createReturnsCreatedResource() {
        // Given
        DeviceResource requestData = TestDataFactory.validDeviceResourceForCreation();
        Device created = TestDataFactory.validDevice();
        when(deviceService.create(any(Device.class))).thenReturn(created);

        // When
        DeviceResource result = deviceController.create(requestData);

        // Then
        assertEquals(created.getId().toString(), result.getId());
        assertEquals(created.getName(), result.getName());
    }

    @Test
    void findByIdReturnsResource() throws DeviceNotFoundException {
        // Given
        Device device = TestDataFactory.validDevice();
        when(deviceService.findById(device.getId())).thenReturn(device);

        // When
        DeviceResource result = deviceController.findById(device.getId());

        // Then
        assertEquals(device.getId().toString(), result.getId());
    }

    @Test
    void findByIdPropagatesException() throws DeviceNotFoundException {
        UUID id = UUID.randomUUID();
        when(deviceService.findById(id)).thenThrow(new DeviceNotFoundException("not found"));

        assertThrows(DeviceNotFoundException.class, () -> deviceController.findById(id));
    }

    @Test
    void findAllByReturnsListOfResources() throws InvalidDeviceStateException {
        // Given
        Device device1 = TestDataFactory.validDevice();
        Device device2 = TestDataFactory.validDevice();
        device2.setId(UUID.randomUUID());
        when(deviceService.findAllBy(device1.getBrand(), device1.getState().toString())).thenReturn(List.of(device1, device2));

        // When
        List<DeviceResource> result = deviceController.findAllBy(device1.getBrand(), device1.getState().toString());

        assertEquals(2, result.size());
        assertEquals(device1.getId().toString(), result.getFirst().getId());
        assertEquals(device2.getId().toString(), result.getLast().getId());
    }

    @Test
    void updateDelegatesToService() throws Exception {
        // Given
        DeviceResource requestData = TestDataFactory.validDeviceResource();
        when(deviceService.update(any(Device.class), eq(UUID.fromString(requestData.getId())))).thenReturn(TestDataFactory.validDevice());
        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);

        // When
        deviceController.update(requestData, UUID.fromString(requestData.getId()));

        // Then
        verify(deviceService).update(captor.capture(), eq(UUID.fromString(requestData.getId())));
        Device captured = captor.getValue();
        assertEquals(requestData.getName(), captured.getName());
        assertEquals(requestData.getBrand(), captured.getBrand());
    }

    @Test
    void updateDeviceNotFoundException() throws Exception {
        DeviceResource requestData = TestDataFactory.validDeviceResource();
        when(deviceService.update(any(Device.class), eq(UUID.fromString(requestData.getId()))))
                .thenThrow(new DeviceNotFoundException("device not found"));

        assertThrows(DeviceNotFoundException.class,
                () -> deviceController.update(requestData, UUID.fromString(requestData.getId())));
    }

    @Test
    void updateDeviceNotValidForUpdateException() throws Exception {
        DeviceResource requestData = TestDataFactory.validDeviceResource();
        when(deviceService.update(any(Device.class), eq(UUID.fromString(requestData.getId()))))
                .thenThrow(new DeviceNotValidForRequestedChangeException("cannot update device in use"));

        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceController.update(requestData, UUID.fromString(requestData.getId())));
    }

    @Test
    void patchDelegatesToService() throws Exception {
        // Given
        Device device = TestDataFactory.validDevice();
        when(deviceService.patch(device.getId(), "New Device", null, null)).thenReturn(device);

        // When
        DeviceResource result = deviceController.patch(device.getId(), "New Device", null, null);

        // Then
        assertEquals(device.getId().toString(), result.getId());
    }

    @Test
    void deleteDelegatesToService() throws Exception {
        // Given
        Device device = TestDataFactory.validDevice();

        // When
        deviceController.delete(device.getId());

        // Then
        verify(deviceService).delete(device.getId());
    }

    @Test
    void deletePropagatesDeviceNotFoundException() throws Exception {
        // Given
        UUID deviceID = UUID.randomUUID();
        doThrow(new DeviceNotFoundException("Device not found")).when(deviceService).delete(deviceID);

        // When - Then
        assertThrows(DeviceNotFoundException.class, () -> deviceController.delete(deviceID));
    }

    @Test
    void deletePropagatesDeviceCannotBeUpdatedException() throws Exception {
        // Given
        UUID deviceID = UUID.randomUUID();
        doThrow(new DeviceNotValidForRequestedChangeException("Device is in use")).when(deviceService).delete(deviceID);

        // When - Then
        assertThrows(DeviceNotValidForRequestedChangeException.class, () -> deviceController.delete(deviceID));
    }
}
