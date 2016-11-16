package com.iodesystems.ssg;

import com.iodesystems.ssg.core.SiteConfigurationException;
import com.iodesystems.ssg.core.StaticSiteGenerator;
import com.mitchellbosecke.pebble.error.PebbleException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

@Mojo(name = "generate")
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/ssg")
    private String sourcePath;

    @Parameter(defaultValue = "${project.build.outputDirectory}/")
    private String outPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        StaticSiteGenerator staticSiteGenerator = new StaticSiteGenerator(new File(sourcePath), new File(outPath));
        try {
            staticSiteGenerator.generate();
        } catch (PebbleException | IOException e) {
            throw new MojoExecutionException("Error generating site", e);
        } catch (SiteConfigurationException e) {
            throw new MojoExecutionException("Error generating site", e);
        }
    }
}
