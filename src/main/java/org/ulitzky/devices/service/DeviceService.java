package org.ulitzky.devices.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ulitzky.devices.dao.DeviceRepository;
import org.ulitzky.devices.exception.DeviceNotFoundException;
import org.ulitzky.devices.exception.DeviceNotValidForRequestedChangeException;
import org.ulitzky.devices.exception.InvalidDeviceStateException;
import org.ulitzky.devices.model.Device;
import org.ulitzky.devices.model.enums.DeviceState;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository deviceRepository;

    @Autowired
    public DeviceService(final DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public Device create(final Device device) {
        Device saved = deviceRepository.save(device);
        log.info("Created device id={} name={} brand={}", saved.getId(), saved.getName(), saved.getBrand());
        return saved;
    }

    public Device findById(final UUID deviceId) throws DeviceNotFoundException {
        try {
            return deviceRepository.findById(deviceId).orElseThrow();
        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("Device not found: id={}", deviceId);
            throw new DeviceNotFoundException("Could not find device with id: " + deviceId);
        }
    }

    public List<Device> findAllBy(final String brand, final String state) throws InvalidDeviceStateException {
        log.debug("Listing devices filtered by brand={} state={}", brand, state);
        DeviceState deviceState = parseDeviceState(state);
        if ((brand != null) && (deviceState != null)) {
            return deviceRepository.findByBrandAndState(brand, deviceState);
        } else if (brand != null) {
            return deviceRepository.findByBrand(brand);
        } else if (deviceState != null) {
            return deviceRepository.findByState(deviceState);
        } else {
            return deviceRepository.findAll();
        }
    }



    @Transactional
    public Device update(final Device updatedDevice, final UUID deviceId)
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
        Device saved = deviceRepository.save(existing);
        log.info("Updated device id={}", deviceId);
        return saved;
    }

    @Transactional
    public Device patch(final UUID deviceId, final String name, final String brand, final String state)
            throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException, InvalidDeviceStateException {
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
            existing.setState(parseDeviceState(state));
        }
        Device saved = deviceRepository.save(existing);
        log.info("Patched device id={}", deviceId);
        return saved;
    }

    @Transactional
    public void delete(final UUID deviceId) throws DeviceNotFoundException, DeviceNotValidForRequestedChangeException {
        Device existing = findById(deviceId);
        checkIfChangeIsPossible(existing, deviceId, true);
        deviceRepository.delete(existing);
        log.info("Deleted device id={}", deviceId);
    }

    private static DeviceState parseDeviceState(final String stateValue) throws InvalidDeviceStateException {
        if (stateValue != null) {
            try {
                return DeviceState.valueOf(stateValue);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid device state requested: {}", stateValue);
                throw new InvalidDeviceStateException("Invalid device state " + stateValue);
            }
        } else {
            return null;
        }
    }

    private void checkIfChangeIsPossible(final Device existing, final UUID deviceId, final boolean isChanging)
            throws DeviceNotValidForRequestedChangeException {
        if (isChanging && !existing.isValidForDeletionOrUpdate()) {
            log.warn("Rejected change for device id={} in state={}", deviceId, existing.getState());
            throw new DeviceNotValidForRequestedChangeException(
                    "Device with id " + deviceId + " is not valid for the requested change.");
        }
    }
}
