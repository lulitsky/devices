package org.ulitzky.devices.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.ulitzky.devices.api.v1.resource.DeviceResource;
import org.ulitzky.devices.model.Device;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeviceMapper {

    @Mapping(target = "id", ignore = true)
    Device mapResourceToEntity(DeviceResource resource);

    DeviceResource mapEntityToResource(Device device);

    List<DeviceResource> mapEntityListToResourceList(List<Device> devices);

    default UUID map(String id) {
        if (id != null) {
            try {
                return UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    default String map(UUID id) {
        return id != null ? id.toString() : null;
    }
}
