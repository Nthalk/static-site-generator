package com.iodesystems.sg.core;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

public class Watcher {
    private final String sourcePath;
    private final Generator generator;
    private final Log log;

    public Watcher(String sourcePath, Generator generator, Log log) {
        this.sourcePath = sourcePath;
        this.generator = generator;
        this.log = log;
    }

    public void start() {
        FileAlterationObserver observer = new FileAlterationObserver(new File(sourcePath));
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                log.info("File created: " + file.getPath());
                generate(generator);
            }

            @Override
            public void onFileChange(File file) {
                log.info("File changed: " + file.getPath());
                generate(generator);
            }

            @Override
            public void onFileDelete(File file) {
                log.info("File deleted: " + file.getPath());
                generate(generator);
            }
        });

        FileAlterationMonitor monitor = new FileAlterationMonitor(1000, observer);

        try {
            log.info("Started watching: " + sourcePath);
            monitor.start();
        } catch (Exception e) {
            log.error("Error watching: " + sourcePath, e);
        }
    }

    private void generate(Generator generator) {
        try {
            generator.generate();
        } catch (ConfigurationException e) {
            log.error("Error generating: " + sourcePath, e);
        }
    }
}
