package com.example.springmigrate.config.utils.error;

public class NodeAlreadyProcessed extends RuntimeException {

    public NodeAlreadyProcessed() {
    }

    public NodeAlreadyProcessed(String message) {
        super(message);
    }
}
