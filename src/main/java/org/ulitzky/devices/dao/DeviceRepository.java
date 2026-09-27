package org.ulitzky.devices.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ulitzky.devices.model.Device;
import org.ulitzky.devices.model.enums.DeviceState;

import java.util.List;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    List<Device> findByBrand(String brand);

    List<Device> findByState(DeviceState state);

    List<Device> findByBrandAndState(String brand, DeviceState state);
}
