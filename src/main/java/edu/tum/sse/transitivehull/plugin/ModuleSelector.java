package edu.tum.sse.transitivehull.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ModuleSelector {
    private static final Logger logger = Logger.getLogger(ModuleSelector.class.getName());
    private static final String POM_XML = "pom.xml";
    private final MavenSession session;
    private Set<MavenProject> selectedProjects = new HashSet<>();

    public ModuleSelector(MavenSession session, Set<MavenProject> selectedProjects) {
        this.session = session;
        this.selectedProjects = selectedProjects;
    }

    public ModuleSelector(MavenSession session) {
        this.session = session;
    }

    public Set<MavenProject> getSelectedProjects() {
        return selectedProjects;
    }

    private List<MavenProject> getProjectsAtPaths(List<Path> paths) {
        return session.getAllProjects().stream()
                .filter(project -> paths.contains(project.getBasedir().toPath().normalize().toAbsolutePath()))
                .collect(Collectors.toList());
    }

    public void selectTransitiveProjects(List<Path> projectPaths) {
        List<MavenProject> mavenProjects = getProjectsAtPaths(projectPaths);
        // Always add modules themselves first.
        selectedProjects.addAll(mavenProjects);

        // Transitively select all upstream modules.
        List<MavenProject> upstreamProjects = getUpstreamProjects(mavenProjects, true);
        selectedProjects.addAll(upstreamProjects);

        // Select direct downstream modules.
        List<MavenProject> downstreamProjects = getDownstreamProjects(mavenProjects, false);
        selectedProjects.addAll(downstreamProjects);

        // Transitively select upstream modules of downstream modules.
        List<MavenProject> upOfDownstreamModules = getUpstreamProjects(downstreamProjects, true);
        selectedProjects.addAll(upOfDownstreamModules);
    }

    public void selectUpstreamProjects(List<Path> paths) {
        List<MavenProject> mavenProjects = getProjectsAtPaths(paths);
        // Always add modules themselves first.
        selectedProjects.addAll(mavenProjects);

        // Transitively select all upstream modules.
        List<MavenProject> upstreamProjects = getUpstreamProjects(mavenProjects, true);
        selectedProjects.addAll(upstreamProjects);
    }

    public List<MavenProject> getUpstreamProjects(List<MavenProject> mavenProjects, boolean transitive) {
        return mavenProjects.stream()
                .map(project -> session.getProjectDependencyGraph().getUpstreamProjects(project, transitive))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<MavenProject> getDownstreamProjects(List<MavenProject> mavenProjects, boolean transitive) {
        return mavenProjects.stream()
                .map(project -> session.getProjectDependencyGraph().getDownstreamProjects(project, transitive))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<Path> getModulePathsFromFiles(List<Path> paths) {
        return paths.stream()
                .map(this::getModulePathsFromFile)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<Path> getModulePathsFromFile(Path path) {
        List<Path> paths = new ArrayList<>();
        try {
            BufferedReader br = Files.newBufferedReader(path);
            paths = br.lines()
                    .filter(line -> !line.equals(""))
                    .map(line -> {
                        line = line.trim();
                        if (line.endsWith(POM_XML)) {
                            line = line.replace(POM_XML, "");
                        }
                        return Paths.get(line).toAbsolutePath();
                    })
                    .collect(Collectors.toList());
        } catch (IOException exception) {
            logger.warning("Could not find any Maven modules in provided file at " + path);
            exception.printStackTrace();
        }
        return paths;
    }

    public void selectDownstreamModules(List<Path> paths) {
        List<MavenProject> mavenProjects = getProjectsAtPaths(paths);
        // Always add modules themselves first.
        selectedProjects.addAll(mavenProjects);

        // Transitively select all downstream modules.
        List<MavenProject> downstreamModules = getDownstreamProjects(mavenProjects, true);
        selectedProjects.addAll(downstreamModules);
    }

    public void printSelectedModules() {
        System.out.println("--------------");
        System.out.println(selectedProjects.stream().map(MavenProject::getName).collect(Collectors.joining("\n")));
        System.out.println("--------------");
    }

    public void writeSelectedModulesToOutput(Path outputFile) {
        try {
            String projectList = selectedProjects.stream()
                    .map(MavenProject::getName)
                    .collect(Collectors.joining("\n"));
            Files.write(outputFile, projectList.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
