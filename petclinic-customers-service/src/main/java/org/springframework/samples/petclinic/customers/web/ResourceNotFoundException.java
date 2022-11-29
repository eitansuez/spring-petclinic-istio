package org.springframework.samples.petclinic.customers.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
	@Serial
  private static final long serialVersionUID = -7037032375889045596L;

	public ResourceNotFoundException(String message) {
        super(message);
    }
}
