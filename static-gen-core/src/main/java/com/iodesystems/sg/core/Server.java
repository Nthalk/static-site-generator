package com.iodesystems.sg.core;

public class Server {

    private final String outPath;
    private final int servePort;

    public Server(String outPath, int servePort) {
        this.outPath = outPath;
        this.servePort = servePort;
    }

    public void start() {
        spark.Spark.externalStaticFileLocation(outPath);
        spark.Spark.port(servePort);
        spark.Spark.init();
    }
}
