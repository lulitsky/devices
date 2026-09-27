package org.ulitzky.devices.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ulitzky.devices.dao.DeviceRepository;
import org.ulitzky.devices.exception.DeviceNotFoundException;
import org.ulitzky.devices.exception.DeviceNotValidForRequestedChangeException;
import org.ulitzky.devices.model.Device;
import org.ulitzky.devices.model.enums.DeviceState;
import org.ulitzky.devices.util.TestDataFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceService deviceService;

    private Device device;

    @BeforeEach
    void init() {
        device = TestDataFactory.validDevice();
    }

    @Test
    void createSavesDevice() {
        // Given
        when(deviceRepository.save(device)).thenReturn(device);

        // When
        Device result = deviceService.create(device);

        // Then
        assertEquals(device, result);
        verify(deviceRepository).save(device);
    }

    @Test
    void findByIdReturnsDevice() throws DeviceNotFoundException {
        // Given
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        // When
        Device result = deviceService.findById(device.getId().toString());

        // Then
        assertEquals(device, result);
    }

    @Test
    void findByIdThrowsNotFoundWhenMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(deviceRepository.findById(id)).thenReturn(Optional.empty());

        // When - Then
        assertThrows(DeviceNotFoundException.class, () -> deviceService.findById(id.toString()));
    }

    @Test
    void findAllByReturnsAllWhenNoFilters() {
        // Given
        when(deviceRepository.findAll()).thenReturn(List.of(device));

        // When
        List<Device> result = deviceService.findAllBy(null, null);

        // Then
        assertEquals(List.of(device), result);
        verify(deviceRepository).findAll();
    }

    @Test
    void findAllByBrand() {
        // Given
        when(deviceRepository.findByBrand("Samsung")).thenReturn(List.of(device));

        // When
        List<Device> result = deviceService.findAllBy("Samsung", null);

        // Then
        assertEquals(List.of(device), result);
        verify(deviceRepository).findByBrand("Samsung");
    }

    @Test
    void findAllByState() {
        // Given
        when(deviceRepository.findByState(DeviceState.AVAILABLE)).thenReturn(List.of(device));

        // When
        List<Device> result = deviceService.findAllBy(null, "AVAILABLE");

        // Then
        assertEquals(List.of(device), result);
        verify(deviceRepository).findByState(DeviceState.AVAILABLE);
    }

    @Test
    void findAllByBrandAndState() {
        // Given
        when(deviceRepository.findByBrandAndState("Samsung", DeviceState.AVAILABLE)).thenReturn(List.of(device));

        // When
        List<Device> result = deviceService.findAllBy("Samsung", "AVAILABLE");

        // Then
        assertEquals(List.of(device), result);
        verify(deviceRepository).findByBrandAndState("Samsung", DeviceState.AVAILABLE);
    }

    @Test
    void updateValid() throws Exception {
        // Given
        Device updatedData = TestDataFactory.validDevice();
        updatedData.setId(device.getId());
        updatedData.setState(DeviceState.IN_USE);
        updatedData.setDateCreated(device.getDateCreated());
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        // When
        Device result = deviceService.update(updatedData, updatedData.getId().toString());

        // Then
        assertEquals(updatedData.getState(), result.getState());
    }

    @Test
    void updateThrowsNotFoundWhenMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(deviceRepository.findById(id)).thenReturn(Optional.empty());

        // When - Then
        assertThrows(DeviceNotFoundException.class, () -> deviceService.update(device, id.toString()));
    }

    @Test
    void updateDeviceNameThrowsNotValidWhenDeviceInUse() {
        // Given
        Device existing = TestDataFactory.validDevice();
        existing.setId(device.getId());
        existing.setState(DeviceState.IN_USE);
        existing.setName("old name");
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(existing));

        // When - Then
        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceService.update(device, device.getId().toString()));
    }


    @Test
    void updateDeviceBrandThrowsNotValidWhenDeviceInUse() {
        // Given
        Device existing = TestDataFactory.validDevice();
        existing.setId(device.getId());
        existing.setState(DeviceState.IN_USE);
        existing.setBrand("Sony");
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(existing));

        // When - Then
        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceService.update(device, device.getId().toString()));
    }

    @Test
    void updateDateCreatedThrowsNotValid() {
        // Given
        Device existing = TestDataFactory.validDevice();
        existing.setId(device.getId());
        existing.setState(DeviceState.AVAILABLE);
        existing.setName(device.getName());
        existing.setBrand(device.getBrand());
        existing.setDateCreated(device.getDateCreated().plusMinutes(10));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(existing));

        // When - Then
        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceService.update(device, device.getId().toString()));
    }




    @Test
    void patchUpdatesOnlyProvidedFields() throws Exception {
        // Given
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);
        String originalBrand = device.getBrand();

        // When
        Device result = deviceService.patch(device.getId().toString(), "New Name", null, null);

        // Then
        assertEquals("New Name", result.getName());
        assertEquals(originalBrand, result.getBrand());
    }

    @Test
    void patchThrowsNotFoundWhenMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(deviceRepository.findById(id)).thenReturn(Optional.empty());

        // When - Then
        assertThrows(DeviceNotFoundException.class, () -> deviceService.patch(id.toString(), "x", null, null));
    }

    @Test
    void patchNameThrowsNotValidWhenDeviceInUse() {
        // Given
        device.setState(DeviceState.IN_USE);
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        // When - Then
        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceService.patch(device.getId().toString(), device.getName() + " new", null, null));
    }

    @Test
    void patchBrandThrowsNotValidWhenDeviceInUse() {
        // Given
        device.setState(DeviceState.IN_USE);
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        // When - Then
        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceService.patch(device.getId().toString(), null, device.getBrand() + " new",  null));
    }

    @Test
    void deleteDeletesValidDevice() throws Exception {
        // Given
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        // When
        deviceService.delete(device.getId().toString());

        // Then
        verify(deviceRepository).delete(device);
    }

    @Test
    void deleteThrowsNotFoundWhenMissing() {
        // Given
        UUID id = UUID.randomUUID();
        when(deviceRepository.findById(id)).thenReturn(Optional.empty());

        // When - Then
        assertThrows(DeviceNotFoundException.class, () -> deviceService.delete(id.toString()));
    }

    @Test
    void deleteThrowsNotValidWhenDeviceInUse() {
        // Given
        device.setState(DeviceState.IN_USE);
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        // When - Then
        assertThrows(DeviceNotValidForRequestedChangeException.class, () -> deviceService.delete(device.getId().toString()));
    }
}
