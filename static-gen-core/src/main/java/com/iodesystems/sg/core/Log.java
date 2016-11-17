package com.iodesystems.sg.core;

public interface Log {
    public void info(String message);

    public void error(String message, Throwable cause);

}
