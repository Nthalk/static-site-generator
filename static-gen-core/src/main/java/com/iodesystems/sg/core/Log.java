package com.iodesystems.sg.core;

public interface Log {
    void info(String message);

    void error(String message, Throwable cause);

}
