package edu.tum.sse.transitivehull.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "transitive-hull", defaultPhase = LifecyclePhase.VALIDATE)
public class TransitiveHullMojo extends AbstractMojo {

    private static final String POM_XML = "pom.xml";

    /**
     * A file containing a list of all changed modules that need to be built.
     * This includes also upstream, downstream, and upstream of downstream modules.
     * Must contain newline-separated lists of relative module paths.
     */
    @Parameter(property = "changedModules", readonly = true, required = true)
    File changedModules;

    /**
     * Files containing modules that were selected as part of test selection.
     * For these modules, we need to build also their upstream modules.
     * Must contain newline-separated lists of relative module paths.
     */
    @Parameter(property = "selectedModules", readonly = true)
    List<File> selectedModules;

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

    private static Path getPath(MavenProject project) {
        return project.getBasedir().toPath().normalize().toAbsolutePath();
    }

    private static List<Path> readProjectPaths(File file) throws IOException {
        BufferedReader br = Files.newBufferedReader(file.toPath());
        return br.lines()
                .filter(line -> !line.equals(""))
                .map(line -> {
                    line = line.trim();
                    if (line.endsWith(POM_XML)) {
                        line = line.replace(POM_XML, "");
                    }
                    return Paths.get(line).toAbsolutePath();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project.isExecutionRoot()) {
            try {
                // Build index of projects, mapped by their absolute path.
                Map<Path, List<MavenProject>> map = createPathMap(session);

                // Create empty set of projects to be built.
                Set<MavenProject> projectsToBeBuilt = new HashSet<>();

                // For changed modules, we select all up-, down-, and up- of downstream modules.
                for (Path projectPath : readProjectPaths(changedModules)) {
                    List<MavenProject> projectsAtPath = new ArrayList<>(map.get(projectPath));
                    for (MavenProject project : projectsAtPath) {
                        // add self
                        projectsToBeBuilt.add(project);

                        // get upstream projects
                        projectsToBeBuilt.addAll(session.getProjectDependencyGraph().getUpstreamProjects(project, true));

                        // get downstream projects
                        List<MavenProject> downstreamProjects = session.getProjectDependencyGraph().getDownstreamProjects(project, true);

                        // get upstream of downstream projects
                        for (MavenProject downstreamProject : downstreamProjects) {
                            // add self
                            projectsToBeBuilt.add(downstreamProject);
                            projectsToBeBuilt.addAll(session.getProjectDependencyGraph().getUpstreamProjects(downstreamProject, true));
                        }
                    }
                }

                // For selected modules, we select all upstream modules.
                Set<Path> selectedProjectPaths = new HashSet<>();
                for (File selectedModuleFile : selectedModules) {
                    selectedProjectPaths.addAll(readProjectPaths(selectedModuleFile));
                }
                for (Path projectPath : selectedProjectPaths) {
                    List<MavenProject> projectsAtPath = new ArrayList<>(map.get(projectPath));
                    for (MavenProject project : projectsAtPath) {
                        // add self
                        projectsToBeBuilt.add(project);
                        // get upstream projects
                        projectsToBeBuilt.addAll(session.getProjectDependencyGraph().getUpstreamProjects(project, true));
                    }
                }
                getLog().info("Selected projects:\n" + projectsToBeBuilt.stream().map(MavenProject::getName).collect(Collectors.joining("\n")));

                // Finally, we need to define the projects for this reactor.
                // TODO: Only using the setters as below will exclude other projects from the final report,
                //  but does still execute all phases for excluded projects.
                session.setProjects(new ArrayList<>(projectsToBeBuilt));
                session.setAllProjects(new ArrayList<>(projectsToBeBuilt));
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        } else {
            getLog().info("I'm not in execution root, skipping.");
        }
    }

    public Map<Path, List<MavenProject>> createPathMap(MavenSession session) {
        return session.getAllProjects().stream().collect(
                Collectors.toMap(
                        TransitiveHullMojo::getPath,
                        Collections::singletonList,
                        (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));
    }
}
