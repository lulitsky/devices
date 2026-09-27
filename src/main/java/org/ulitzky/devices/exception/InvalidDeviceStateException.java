package org.ulitzky.devices.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid device state provided")
public class InvalidDeviceStateException extends Exception {

    public InvalidDeviceStateException(String message) {
        super(message);
    }
}
