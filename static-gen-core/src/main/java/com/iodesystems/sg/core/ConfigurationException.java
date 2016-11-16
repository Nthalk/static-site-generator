package com.iodesystems.sg.core;

public class ConfigurationException extends Throwable {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable e) {
        super(message, e);
    }
}
