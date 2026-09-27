package org.ulitzky.devices.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
        if ((brand != null) && (state != null)) {
            return deviceRepository.findByBrandAndState(brand, DeviceState.valueOf(state));
        } else if (brand != null) {
            return deviceRepository.findByBrand(brand);
        } else if (state != null) {
            return deviceRepository.findByState(DeviceState.valueOf(state));
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
        if ((!existing.isValidForDeletionOrUpdate()) &&
                ((!updatedDevice.getName().equals(existing.getName())) || (!updatedDevice.getBrand().equals(existing.getBrand())))){
            throw new DeviceNotValidForRequestedChangeException("Device with id " + deviceId + " is not valid for the requested change.");
        }
        existing.setName(updatedDevice.getName());
        existing.setBrand(updatedDevice.getBrand());
        existing.setState(updatedDevice.getState());
        return deviceRepository.save(existing);
    }

    public Device patch(final String deviceId, final String name, final String brand, final String state)
            throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException {
        Device existing = findById(deviceId);

        if (name != null) {
            if (!existing.isValidForDeletionOrUpdate() && !name.equals(existing.getName())) {
                throw new DeviceNotValidForRequestedChangeException("Cannot update name for device in use");
            } else {
                existing.setName(name);
            }
        }
        if (brand != null) {
            if (!existing.isValidForDeletionOrUpdate() && !brand.equals(existing.getBrand())) {
                throw new DeviceNotValidForRequestedChangeException("Cannot update brand for device in use");
            } else {
                existing.setBrand(brand);
            }
        }
        if (state != null) {
            // TODO check that state is valid
            existing.setState(DeviceState.valueOf(state));
        }
        return deviceRepository.save(existing);
    }

    public void delete(final String deviceId) throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException {
        Device existing = findById(deviceId);
        if (!existing.isValidForDeletionOrUpdate()) {
            throw new DeviceNotValidForRequestedChangeException("Device with id " + deviceId + " is not valid for deletion.");
        }
        deviceRepository.delete(existing);
    }
}
