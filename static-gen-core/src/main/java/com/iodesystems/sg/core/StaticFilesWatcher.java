package com.iodesystems.sg.core;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;

public class StaticFilesWatcher {
    private final String sourcePath;
    private final StaticFilesGenerator staticFilesGenerator;
    private final Log log;

    public StaticFilesWatcher(String sourcePath, StaticFilesGenerator staticFilesGenerator, Log log) {
        this.sourcePath = sourcePath;
        this.staticFilesGenerator = staticFilesGenerator;
        this.log = log;
    }

    public void start() {
        FileAlterationObserver observer = new FileAlterationObserver(new File(sourcePath));
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                log.info("File created: " + file.getPath());
                generate(staticFilesGenerator);
            }

            @Override
            public void onFileChange(File file) {
                log.info("File changed: " + file.getPath());
                generate(staticFilesGenerator);
            }

            @Override
            public void onFileDelete(File file) {
                log.info("File deleted: " + file.getPath());
                generate(staticFilesGenerator);
            }
        });

        FileAlterationMonitor monitor = new FileAlterationMonitor(1000, observer);
        try {
            log.info("Started watching: " + sourcePath);
            monitor.start();
        } catch (InterruptedException e) {
            try {
                log.info("Stopped watching: " + sourcePath);
                monitor.stop();
            } catch (Exception e1) {
            }
        } catch (Exception e) {
        }
    }

    private void generate(StaticFilesGenerator staticFilesGenerator) {
        try {
            log.info("Generating: " + sourcePath);
            staticFilesGenerator.generate();
        } catch (ConfigurationException e) {

        }
    }
}
