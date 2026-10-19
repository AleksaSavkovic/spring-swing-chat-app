package org.chatapp.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "You don't have access to this resource.")
public class ResourceForbiddenException extends RuntimeException {
    public ResourceForbiddenException(String message) {
        super(message);
    }

    public ResourceForbiddenException() {

    }
}
