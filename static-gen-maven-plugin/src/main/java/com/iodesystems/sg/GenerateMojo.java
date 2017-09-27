package com.iodesystems.sg;

import com.iodesystems.sg.core.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "generate")
public class GenerateMojo extends AbstractMojo {

    @Parameter(
        defaultValue = "${project.basedir}/src/main/static",
        property = "sg.src")
    protected String sourcePath;

    @Parameter(
        defaultValue = "${project.build.outputDirectory}",
        property = "sg.out")
    protected String outPath;

    @Parameter(
        defaultValue = "manifest.yml",
        property = "sg.manifest")
    protected String manifest;

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
        Generator generator = new Generator(
            new File(sourcePath),
            new File(outPath),
            manifest);

        generate(generator);

        if (serve) {
            getLog().info("Serving files from " + outPath);
            new Thread(() -> new Server(outPath, servePort).start()).start();
        }
        if (watch) {
            getLog().info("Watching files from " + outPath);
            new Watcher(sourcePath, generator, new Log() {
                @Override
                public void info(String message) {
                    getLog().info(message);
                }

                @Override
                public void error(String message, Throwable cause) {
                    getLog().error(message, cause);
                }
            }).start();
        }

        if (watch || serve) {
            delay();
        }
    }


    private void delay() {
        while (true) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
            }
        }
    }

    private void generate(Generator generator) {
        try {
            generator.generate();
        } catch (ConfigurationException e) {
            getLog().error("Could not generate files", e);
        }
    }
}
