package org.ulitzky.devices.service.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import org.ulitzky.devices.api.v1.resource.DeviceResource;
import org.ulitzky.devices.api.v1.resource.DeviceState;
import org.ulitzky.devices.model.Device;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-09-27T15:30:13+0200",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.11 (Homebrew)"
)
@Component
public class DeviceMapperImpl implements DeviceMapper {

    @Override
    public Device mapResourceToEntity(DeviceResource resource) {
        if ( resource == null ) {
            return null;
        }

        Device device = new Device();

        device.setId( map( resource.getId() ) );
        device.setName( resource.getName() );
        device.setBrand( resource.getBrand() );
        device.setState( deviceStateToDeviceState( resource.getState() ) );
        device.setDateCreated( resource.getDateCreated() );

        return device;
    }

    @Override
    public DeviceResource mapEntityToResource(Device device) {
        if ( device == null ) {
            return null;
        }

        DeviceResource deviceResource = new DeviceResource();

        deviceResource.setId( map( device.getId() ) );
        deviceResource.setName( device.getName() );
        deviceResource.setBrand( device.getBrand() );
        deviceResource.setState( deviceStateToDeviceState1( device.getState() ) );
        deviceResource.setDateCreated( device.getDateCreated() );

        return deviceResource;
    }

    @Override
    public List<DeviceResource> mapEntityListToResourceList(List<Device> devices) {
        if ( devices == null ) {
            return null;
        }

        List<DeviceResource> list = new ArrayList<DeviceResource>( devices.size() );
        for ( Device device : devices ) {
            list.add( mapEntityToResource( device ) );
        }

        return list;
    }

    protected org.ulitzky.devices.model.enums.DeviceState deviceStateToDeviceState(DeviceState deviceState) {
        if ( deviceState == null ) {
            return null;
        }

        org.ulitzky.devices.model.enums.DeviceState deviceState1;

        switch ( deviceState ) {
            case AVAILABLE: deviceState1 = org.ulitzky.devices.model.enums.DeviceState.AVAILABLE;
            break;
            case IN_USE: deviceState1 = org.ulitzky.devices.model.enums.DeviceState.IN_USE;
            break;
            case INACTIVE: deviceState1 = org.ulitzky.devices.model.enums.DeviceState.INACTIVE;
            break;
            default: throw new IllegalArgumentException( "Unexpected enum constant: " + deviceState );
        }

        return deviceState1;
    }

    protected DeviceState deviceStateToDeviceState1(org.ulitzky.devices.model.enums.DeviceState deviceState) {
        if ( deviceState == null ) {
            return null;
        }

        DeviceState deviceState1;

        switch ( deviceState ) {
            case AVAILABLE: deviceState1 = DeviceState.AVAILABLE;
            break;
            case IN_USE: deviceState1 = DeviceState.IN_USE;
            break;
            case INACTIVE: deviceState1 = DeviceState.INACTIVE;
            break;
            default: throw new IllegalArgumentException( "Unexpected enum constant: " + deviceState );
        }

        return deviceState1;
    }
}
