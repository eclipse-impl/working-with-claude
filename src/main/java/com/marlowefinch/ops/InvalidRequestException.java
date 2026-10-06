package com.marlowefinch.ops;

import java.util.List;

/** Carries every validation problem found in one request; rendered as a 400 by {@link ApiExceptionHandler}. */
public class InvalidRequestException extends RuntimeException {

    private final List<String> errors;

    public InvalidRequestException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }

    public static void throwIfAny(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
    }
}
