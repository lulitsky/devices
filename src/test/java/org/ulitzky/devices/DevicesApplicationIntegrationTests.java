package org.ulitzky.devices;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.ulitzky.devices.api.v1.controller.DeviceController;
import org.ulitzky.devices.api.v1.resource.DeviceResource;
import org.ulitzky.devices.api.v1.resource.DeviceState;
import org.ulitzky.devices.exception.DeviceNotFoundException;
import org.ulitzky.devices.exception.DeviceNotValidForRequestedChangeException;
import org.ulitzky.devices.exception.InvalidDeviceStateException;
import org.ulitzky.devices.util.TestDataFactory;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DevicesApplicationIntegrationTests {

    @Autowired
    private DeviceController deviceController;

    @Test
    void testCreateDeviceAndFetchItByAllMeans() throws DeviceNotFoundException, InvalidDeviceStateException {
        List<DeviceResource> foundAllBeforeCreation = deviceController.findAllBy(null, null);
        assertTrue(foundAllBeforeCreation.isEmpty());

        DeviceResource newDevice = createAndFetchDevice();

        List<DeviceResource> foundAllAfterCreation = deviceController.findAllBy(null, null);
        assertEquals(1, foundAllAfterCreation.size());
        assertEquals(newDevice.getId(), foundAllAfterCreation.getFirst().getId());

        List<DeviceResource> foundByBrandAfterCreation = deviceController.findAllBy(newDevice.getBrand(), null);
        assertEquals(1, foundByBrandAfterCreation.size());
        assertEquals(newDevice.getId(), foundByBrandAfterCreation.getFirst().getId());
        assertEquals(newDevice.getBrand(), foundByBrandAfterCreation.getFirst().getBrand());

        List<DeviceResource> foundByStateAfterCreation = deviceController.findAllBy(null, newDevice.getState().toString());
        assertEquals(1, foundByStateAfterCreation.size());
        assertEquals(newDevice.getId(), foundByStateAfterCreation.getFirst().getId());
        assertEquals(newDevice.getState().toString(), foundByStateAfterCreation.getFirst().getState().toString());

        List<DeviceResource> foundByBrandAndStateAfterCreation = deviceController.findAllBy(newDevice.getBrand(), newDevice.getState().toString());
        assertEquals(1, foundByBrandAndStateAfterCreation.size());
        assertEquals(newDevice.getId(), foundByBrandAndStateAfterCreation.getFirst().getId());
        assertEquals(newDevice.getBrand(), foundByBrandAndStateAfterCreation.getFirst().getBrand());
        assertEquals(newDevice.getState().toString(), foundByBrandAndStateAfterCreation.getFirst().getState().toString());
    }


    @Test
    void testDeviceIsNotFoundAfterDelete() throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException, InvalidDeviceStateException {
        DeviceResource newDevice = createAndFetchDevice();

        deviceController.delete(UUID.fromString(newDevice.getId()));

        List<DeviceResource> foundAllAfterDelete = deviceController.findAllBy(null, null);
        assertTrue(foundAllAfterDelete.isEmpty());

        List<DeviceResource> foundByBrandAfterDelete = deviceController.findAllBy(newDevice.getBrand(), null);
        assertTrue(foundByBrandAfterDelete.isEmpty());

        List<DeviceResource> foundByStateAfterDelete = deviceController.findAllBy(null, newDevice.getState().toString());
        assertTrue(foundByStateAfterDelete.isEmpty());

        List<DeviceResource> foundByBrandStateAfterDelete = deviceController.findAllBy(newDevice.getBrand(), newDevice.getState().toString());
        assertTrue(foundByBrandStateAfterDelete.isEmpty());
    }

    @Test
    void testCannotDeleteDeviceInUse() {
        DeviceResource deviceData = TestDataFactory.inUseDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);
        assertEquals(DeviceState.IN_USE, created.getState());

        assertThrows(DeviceNotValidForRequestedChangeException.class, () -> deviceController.delete(UUID.fromString(created.getId())));
    }

    @Test
    void testUpdateAvailableDevice() throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException {
        DeviceResource deviceData = TestDataFactory.validDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);

        deviceData.setBrand("New Brand");
        deviceData.setName("New name");
        deviceData.setState(DeviceState.INACTIVE);

        DeviceResource updated = deviceController.update(deviceData, UUID.fromString(created.getId()));

        assertEquals(created.getId(), updated.getId());
        assertEquals(created.getDateCreated(), updated.getDateCreated());

        assertEquals(deviceData.getName(), updated.getName());
        assertEquals(deviceData.getBrand(), updated.getBrand());
        assertEquals(deviceData.getState(), updated.getState());

        DeviceResource foundAfterUpdate = deviceController.findById(UUID.fromString(created.getId()));
        assertEquals(deviceData.getName(), foundAfterUpdate.getName());
        assertEquals(deviceData.getBrand(), foundAfterUpdate.getBrand());
        assertEquals(deviceData.getState(), foundAfterUpdate.getState());
    }

    @Test
    void testCannotUpdateDateCreated()  {
        DeviceResource deviceData = TestDataFactory.validDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);

        deviceData.setDateCreated(created.getDateCreated().minusMinutes(30));
        deviceData.setName("New name");

        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceController.update(deviceData, UUID.fromString(created.getId())));
    }

    @Test
    void testCannotUpdateNameForDeviceInUse()  {
        DeviceResource deviceData = TestDataFactory.inUseDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);
        assertEquals(DeviceState.IN_USE, created.getState());

        deviceData.setName("New name");

        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceController.update(deviceData, UUID.fromString(created.getId())));
    }

    @Test
    void testCannotUpdateBrandForDeviceInUse() {
        DeviceResource deviceData = TestDataFactory.inUseDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);
        assertEquals(DeviceState.IN_USE, created.getState());

        deviceData.setBrand("NewBrand");

        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceController.update(deviceData, UUID.fromString(created.getId())));
    }

    @Test
    void testCanUpdateStateForDeviceInUse() throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException, InvalidDeviceStateException {
        DeviceResource deviceData = TestDataFactory.inUseDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);
        assertEquals(DeviceState.IN_USE, created.getState());

        deviceData.setState(DeviceState.INACTIVE);

        DeviceResource updated = deviceController.update(deviceData, UUID.fromString(created.getId()));
        assertEquals(created.getId(), updated.getId());
        assertEquals(created.getDateCreated(), updated.getDateCreated());
        assertEquals(deviceData.getName(), updated.getName());
        assertEquals(deviceData.getBrand(), updated.getBrand());
        assertEquals(DeviceState.INACTIVE, updated.getState());

        DeviceResource foundAfterUpdate = deviceController.findById(UUID.fromString(created.getId()));
        assertEquals(DeviceState.INACTIVE, foundAfterUpdate.getState());

        List<DeviceResource> devicesInactive = deviceController.findAllBy(null, DeviceState.INACTIVE.toString());
        assertEquals(1, devicesInactive.size());
        assertEquals(updated.getId(), devicesInactive.getFirst().getId());
    }


    @Test
    void testPatchMultipleFieldsForAvailableDevice() throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException, InvalidDeviceStateException {
        DeviceResource deviceData = TestDataFactory.validDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);

        DeviceResource patched = deviceController.patch(UUID.fromString(created.getId()), "NewName", "NewBrand", DeviceState.IN_USE.toString());
        assertEquals(created.getId(), patched.getId());
        assertEquals(created.getDateCreated(), patched.getDateCreated());
        assertEquals("NewName", patched.getName());
        assertEquals("NewBrand", patched.getBrand());
        assertEquals(DeviceState.IN_USE, patched.getState());
    }

    @Test
    void testPatchNameForAvailableDevice() throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException, InvalidDeviceStateException {
        DeviceResource deviceData = TestDataFactory.validDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);

        DeviceResource patched = deviceController.patch(UUID.fromString(created.getId()), "Patched Name", null, null);
        assertEquals(created.getId(), patched.getId());
        assertEquals(created.getDateCreated(), patched.getDateCreated());
        assertEquals("Patched Name", patched.getName());
        assertEquals(created.getBrand(), patched.getBrand());
        assertEquals(created.getState(), patched.getState());
    }

    @Test
    void testPatchBrandForAvailableDevice() throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException, InvalidDeviceStateException {
        DeviceResource deviceData = TestDataFactory.validDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);

        DeviceResource patched = deviceController.patch(UUID.fromString(created.getId()), null, "Brand2", null);
        assertEquals(created.getId(), patched.getId());
        assertEquals(created.getDateCreated(), patched.getDateCreated());
        assertEquals(created.getName(), patched.getName());
        assertEquals("Brand2", patched.getBrand());
        assertEquals(created.getState(), patched.getState());
    }

    @Test
    void testPatchStateForAvailableDevice() throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException, InvalidDeviceStateException {
        DeviceResource deviceData = TestDataFactory.validDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);

        DeviceResource patched = deviceController.patch(UUID.fromString(created.getId()), null, null, DeviceState.INACTIVE.toString());
        assertEquals(created.getId(), patched.getId());
        assertEquals(created.getDateCreated(), patched.getDateCreated());
        assertEquals(created.getName(), patched.getName());
        assertEquals(created.getBrand(), patched.getBrand());
        assertEquals(DeviceState.INACTIVE, patched.getState());
    }

    @Test
    void testCannotPatchNameForDeviceInUse()  {
        DeviceResource deviceData = TestDataFactory.inUseDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);
        assertEquals(DeviceState.IN_USE, created.getState());

        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceController.patch(UUID.fromString(created.getId()), "New name", null, null));
    }

    @Test
    void testCannotPatchBrandForDeviceInUse()  {
        DeviceResource deviceData = TestDataFactory.inUseDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);
        assertEquals(DeviceState.IN_USE, created.getState());

        assertThrows(DeviceNotValidForRequestedChangeException.class,
                () -> deviceController.patch(UUID.fromString(created.getId()), null, "NewBrand", null));
    }

    @Test
    void testCanPatchStateForDeviceInUse() throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException, InvalidDeviceStateException {
        DeviceResource deviceData = TestDataFactory.inUseDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);
        assertEquals(DeviceState.IN_USE, created.getState());

       DeviceResource patched = deviceController.patch(UUID.fromString(created.getId()), null, null, DeviceState.INACTIVE.toString());
       assertEquals(DeviceState.INACTIVE, patched.getState());
       assertEquals(created.getName(), patched.getName());
       assertEquals(created.getBrand(), patched.getBrand());

        DeviceResource foundAfterPatch = deviceController.findById(UUID.fromString(created.getId()));
        assertEquals(DeviceState.INACTIVE, foundAfterPatch.getState());

        List<DeviceResource> devicesInactive = deviceController.findAllBy(null, DeviceState.INACTIVE.toString());
        assertEquals(1, devicesInactive.size());
        assertEquals(patched.getId(), devicesInactive.getFirst().getId());
        assertEquals(DeviceState.INACTIVE, devicesInactive.getFirst().getState());
    }

    @Test
    void testFindByIdForNotFoundDevice() {
        assertThrows(DeviceNotFoundException.class, () -> deviceController.findById(UUID.randomUUID()));
    }

    @Test
    void testDeleteForNotFoundDevice() {
        assertThrows(DeviceNotFoundException.class, () -> deviceController.delete((UUID.randomUUID())));
    }

    @Test
    void testPatchForNotFoundDevice() {
        assertThrows(DeviceNotFoundException.class, () -> deviceController.patch(UUID.randomUUID(), "New Name", null, null));
    }

    private @NonNull DeviceResource createAndFetchDevice() throws DeviceNotFoundException {
        DeviceResource deviceData = TestDataFactory.validDeviceResourceForCreation();
        DeviceResource created = deviceController.create(deviceData);
        assertNotNull(created.getId());
        assertNotNull(created.getDateCreated());
        assertEquals(deviceData.getName(), created.getName());
        assertEquals(deviceData.getBrand(), created.getBrand());
        assertEquals(deviceData.getState().toString(), created.getState().toString());

        DeviceResource fetchedById = deviceController.findById(UUID.fromString(created.getId()));
        assertNotNull(fetchedById);
        assertEquals(fetchedById.getId(), created.getId());
        assertEquals(fetchedById.getName(), created.getName());
        assertEquals(fetchedById.getBrand(), created.getBrand());
        assertEquals(fetchedById.getState(), created.getState());
        assertEquals(fetchedById.getDateCreated(), created.getDateCreated());

        return fetchedById;
    }

}
