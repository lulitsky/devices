package org.ulitzky.devices.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ulitzky.devices.dao.DeviceRepository;
import org.ulitzky.devices.exception.DeviceNotFoundException;
import org.ulitzky.devices.exception.DeviceNotValidForRequestedChangeException;
import org.ulitzky.devices.model.Device;
import org.ulitzky.devices.model.enums.DeviceState;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Autowired
    public DeviceService(final DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Device create(final Device device) {
        return deviceRepository.save(device);
    }

    public Device findById(final String deviceId) throws DeviceNotFoundException {
        try {
            return deviceRepository.findById(UUID.fromString(deviceId)).orElseThrow();
        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw new DeviceNotFoundException("Could not find device with id: " + deviceId);
        }
    }

    public List<Device> findAllBy(final String brand, final String state) {
        // TODO check that state is valid
        DeviceState parsedState = state != null ? DeviceState.valueOf(state) : null;
        if ((brand != null) && (parsedState != null)) {
            return deviceRepository.findByBrandAndState(brand, parsedState);
        } else if (brand != null) {
            return deviceRepository.findByBrand(brand);
        } else if (parsedState != null) {
            return deviceRepository.findByState(parsedState);
        } else {
            return deviceRepository.findAll();
        }
    }

    public Device update(final Device updatedDevice, final String deviceId)
            throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException {
        Device existing = findById(deviceId);
        if ((updatedDevice.getDateCreated() != null) &&
                (!updatedDevice.getDateCreated().truncatedTo(ChronoUnit.MINUTES)
                        .equals(existing.getDateCreated().truncatedTo(ChronoUnit.MINUTES)))) {
            throw new DeviceNotValidForRequestedChangeException("Device created time cannot be updated.");
        }
        checkIfChangeIsPossible(existing, deviceId, !updatedDevice.getName().equals(existing.getName())
                || !updatedDevice.getBrand().equals(existing.getBrand()));
        existing.setName(updatedDevice.getName());
        existing.setBrand(updatedDevice.getBrand());
        existing.setState(updatedDevice.getState());
        return deviceRepository.save(existing);
    }

    public Device patch(final String deviceId, final String name, final String brand, final String state)
            throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException {
        Device existing = findById(deviceId);

        if (name != null) {
            checkIfChangeIsPossible(existing, deviceId, !name.equals(existing.getName()));
            existing.setName(name);
        }
        if (brand != null) {
            checkIfChangeIsPossible(existing, deviceId, !brand.equals(existing.getBrand()));
            existing.setBrand(brand);
        }
        if (state != null) {
            // TODO check that state is valid
            existing.setState(DeviceState.valueOf(state));
        }
        return deviceRepository.save(existing);
    }

    public void delete(final String deviceId) throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException {
        Device existing = findById(deviceId);
        checkIfChangeIsPossible(existing, deviceId, true);
        deviceRepository.delete(existing);
    }

    private void checkIfChangeIsPossible(final Device existing, final String deviceId, final boolean isChanging)
            throws DeviceNotValidForRequestedChangeException {
        if (isChanging && !existing.isValidForDeletionOrUpdate()) {
            throw new DeviceNotValidForRequestedChangeException(
                    "Device with id " + deviceId + " is not valid for the requested change.");
        }
    }
}
