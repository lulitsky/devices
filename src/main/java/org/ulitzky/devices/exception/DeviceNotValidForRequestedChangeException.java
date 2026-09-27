package org.ulitzky.devices.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Device is not valid for the requested change.")
public class DeviceNotValidForRequestedChangeException extends Exception {

    public DeviceNotValidForRequestedChangeException(String message) {
        super(message);
    }
}
