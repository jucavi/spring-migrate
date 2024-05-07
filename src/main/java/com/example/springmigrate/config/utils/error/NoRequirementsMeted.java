package com.example.springmigrate.config.utils.error;

public class NoRequirementsMeted extends RuntimeException {

    public NoRequirementsMeted() {
    }

    public NoRequirementsMeted(String message) {
        super(message);
    }
}
