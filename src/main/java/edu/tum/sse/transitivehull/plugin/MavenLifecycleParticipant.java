package edu.tum.sse.transitivehull.plugin;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "Selects reactor modules from files.")
public class MavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private static final Logger logger = Logger.getLogger(MavenLifecycleParticipant.class.getName());
    private static final String PARAMETER_TRANSITIVE_MODULES = "transitiveModules";
    private static final String PARAMETER_UPSTREAM_MODULES = "upstreamModules";
    private static final String PARAMETER_DOWNSTREAM_MODULES = "downstreamModules";
    private static final String PARAMETER_ACTIVATE_MODULE_SELECTION = "useModuleSelection";
    private static final String PARAMETER_OUTPUT_FILE = "moduleSelectionOutput";
    private static final String FILE_PATH_DELIMITER = ",";

    private static List<Path> getValidFilePathsFromString(String filePathString) {
        return Arrays.stream(filePathString.split(FILE_PATH_DELIMITER))
                .map(filePath -> Paths.get(filePath).toAbsolutePath())
                .filter(Files::exists)
                .collect(Collectors.toList());
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        Properties userProperties = session.getRequest().getUserProperties();
        if (session.getCurrentProject().isExecutionRoot()) {
            logger.info("Howdy, you activated Maven module selection.");
            logger.info("Use -DtransitiveModules=transitive-modules.txt,... to define modules, " +
                    "that should be transitively selected.");
            logger.info("Use -DupstreamModules=upstream-modules.txt,... to define modules, " +
                    "for which only upstream modules should be selected.");
            logger.info("Use -DdownstreamModules=downstream-modules.txt,... to define modules, " +
                    "for which only downstream modules should be selected.");

            ModuleSelector moduleSelector = new ModuleSelector(session);
            if (userProperties.containsKey(PARAMETER_TRANSITIVE_MODULES)) {
                List<Path> filePaths = getValidFilePathsFromString(userProperties.getProperty(PARAMETER_TRANSITIVE_MODULES));
                logger.info(String.format("Found %d valid transitive module file(s): %s", filePaths.size(), filePaths));
                List<Path> modulePaths = moduleSelector.getModulePathsFromFiles(filePaths);
                moduleSelector.selectTransitiveProjects(modulePaths);
                logger.info(String.format("Selected module(s) (%d) after transitive selection:", moduleSelector.getSelectedProjects().size()));
                moduleSelector.printSelectedModules();
            }
            if (userProperties.containsKey(PARAMETER_UPSTREAM_MODULES)) {
                List<Path> filePaths = getValidFilePathsFromString(userProperties.getProperty(PARAMETER_UPSTREAM_MODULES));
                logger.info(String.format("Found %d valid upstream module file(s): %s", filePaths.size(), filePaths));
                List<Path> modulePaths = moduleSelector.getModulePathsFromFiles(filePaths);
                moduleSelector.selectUpstreamProjects(modulePaths);
                logger.info(String.format("Selected module(s) (%d) after upstream selection:", moduleSelector.getSelectedProjects().size()));
                moduleSelector.printSelectedModules();
            }
            if (userProperties.containsKey(PARAMETER_DOWNSTREAM_MODULES)) {
                List<Path> filePaths = getValidFilePathsFromString(userProperties.getProperty(PARAMETER_DOWNSTREAM_MODULES));
                logger.info(String.format("Found %d valid downstream module file(s): %s", filePaths.size(), filePaths));
                List<Path> modulePaths = moduleSelector.getModulePathsFromFiles(filePaths);
                moduleSelector.selectDownstreamModules(modulePaths);
                logger.info(String.format("Selected module(s) (%d) after downstream selection:", moduleSelector.getSelectedProjects().size()));
                moduleSelector.printSelectedModules();
            }
            if (userProperties.containsKey(PARAMETER_OUTPUT_FILE)) {
                Path outputFile = Paths.get(userProperties.getProperty(PARAMETER_OUTPUT_FILE)).toAbsolutePath();
                moduleSelector.writeSelectedModulesToOutput(outputFile);
            }
            if (userProperties.containsKey(PARAMETER_ACTIVATE_MODULE_SELECTION)) {
                session.setProjects(new ArrayList<>(moduleSelector.getSelectedProjects()));
            }
        }
    }
}
