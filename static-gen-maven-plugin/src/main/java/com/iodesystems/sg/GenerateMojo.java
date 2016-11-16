package com.iodesystems.sg;

import com.iodesystems.sg.core.SiteConfigurationException;
import com.iodesystems.sg.core.StaticSiteGenerator;
import com.mitchellbosecke.pebble.error.PebbleException;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

@Mojo(name = "generate")
public class GenerateMojo extends AbstractMojo {

    @Parameter(
        defaultValue = "${project.basedir}/src/main/static",
        property = "sg.src")
    protected String sourcePath;

    @Parameter(
        defaultValue = "${project.build.outputDirectory}/",
        property = "sg.out")
    protected String outPath;

    @Parameter(
        defaultValue = "false",
        property = "sg.watch")
    protected Boolean watch;

    @Parameter(
        defaultValue = "false",
        property = "sg.serve")
    protected Boolean serve;

    @Parameter(
        defaultValue = "3000",
        property = "sg.servePort")
    protected Integer servePort;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        generate();

        if (serve) {
            serve();
        }

        if (watch) {
            watch();
        }
        if (watch || serve) {
            delay();
        }
    }

    private void serve() {
        getLog().info("Serving files from " + outPath);
        spark.Spark.externalStaticFileLocation(outPath);
        spark.Spark.port(servePort);
        new Thread(new Runnable() {
            @Override
            public void run() {
                spark.Spark.init();
            }
        }).start();
    }

    private void delay() {
        while (true) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void watch() {
        FileAlterationObserver observer = new FileAlterationObserver(new File(sourcePath));
        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                getLog().info("File created: " + file.getPath());
                generate();
            }

            @Override
            public void onFileChange(File file) {
                getLog().info("File changed: " + file.getPath());
                generate();
            }

            @Override
            public void onFileDelete(File file) {
                getLog().info("File deleted: " + file.getPath());
                generate();
            }
        });

        FileAlterationMonitor monitor = new FileAlterationMonitor(1000, observer);
        try {
            monitor.start();
            getLog().info("Watching for changes: " + sourcePath);
        } catch (InterruptedException e) {
            getLog().info("Stopped watching for changes");
            try {
                monitor.stop();
            } catch (Exception e1) {
                getLog().error("Stopped watching for changes", e);
            }
        } catch (Exception e) {
            getLog().error("Stopped watching for changes", e);
        }
    }

    private void generate() {
        StaticSiteGenerator staticSiteGenerator = new StaticSiteGenerator(new File(sourcePath), new File(outPath));
        try {
            staticSiteGenerator.generate();
        } catch (PebbleException | IOException e) {
            e.printStackTrace();
        } catch (SiteConfigurationException e) {
            e.printStackTrace();
        }
    }
}
