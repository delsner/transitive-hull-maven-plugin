package edu.tum.sse.transitivehull.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

@Mojo(name = "transitive-hull", defaultPhase = LifecyclePhase.VALIDATE)
public class TransitiveHullMojo extends AbstractMojo {

    @Parameter(property = "transitiveModules", readonly = true)
    List<File> transitiveModules;

    @Parameter(property = "upstreamModules", readonly = true)
    List<File> upstreamModules;

    @Parameter(property = "downstreamModules", readonly = true)
    List<File> downstreamModules;

    /**
     * The current project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * The Maven session.
     */
    @Parameter(defaultValue = "${session}")
    MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Not implemented.");
    }
}
