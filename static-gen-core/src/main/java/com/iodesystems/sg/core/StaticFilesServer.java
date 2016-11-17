package com.iodesystems.sg.core;

public class StaticFilesServer {

    private final String outPath;
    private final int servePort;

    public StaticFilesServer(String outPath, int servePort) {
        this.outPath = outPath;
        this.servePort = servePort;
    }

    public void start() {
        spark.Spark.externalStaticFileLocation(outPath);
        spark.Spark.port(servePort);
        spark.Spark.init();
    }
}
