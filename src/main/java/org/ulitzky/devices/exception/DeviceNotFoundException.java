package org.ulitzky.devices.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Could not find device with id.")
public class DeviceNotFoundException extends Exception {

    public DeviceNotFoundException(String message) {
        super(message);
    }
}
