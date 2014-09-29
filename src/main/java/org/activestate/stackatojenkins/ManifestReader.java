package org.activestate.stackatojenkins;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.List;
import java.util.Map;

public class ManifestReader {



    private File manifestFile;
    private List<Map<String, Object>> applicationList;

    public ManifestReader(File manifestFile) throws ManifestParsingException, IOException, InterruptedException {
        this.manifestFile = manifestFile;
        this.applicationList = parseManifest();
    }

    /**
     * Returns a DeploymentInfo object containing all the information of a single app
     * that was in the manifest file, including the global information applicable.
     *
     * @param appName the application name
     * @return the deployment info
     */
    public Map<String, Object> getApplicationInfo(String appName) throws ManifestParsingException {
        return getApplicationMap(appName);
        // TODO: get global map and combine both global and app map
    }

    /**
     * Returns the Map of exclusive deployment info of an app, given its name.
     * If no name given (null), will use the first app.
     */
    private Map<String, Object> getApplicationMap(String appName) throws ManifestParsingException {
        // With no parameter, return the first application.
        if (appName == null) {
            return applicationList.get(0);
        } else {
            for (Map<String, Object> app : applicationList) {
                String name = (String) app.get("name");
                if (name != null && name.equals(appName)) {
                    return app;
                }
            }
            throw new ManifestParsingException("Manifest file does not contain an app named " + appName + ".");
        }
    }

    // TODO: getGlobalMap()

    /**
     * Returns the list of maps describing the applications.
     */
    private List<Map<String, Object>> parseManifest() throws IOException, ManifestParsingException, InterruptedException {
        InputStream inputStream = new FileInputStream(manifestFile);
        Yaml yaml = new Yaml();
        Object parsedYaml = yaml.load(inputStream);
        Map<String, List<Map<String, Object>>> parsedYamlMap;
        if (parsedYaml instanceof Map<?, ?>) {
            parsedYamlMap = (Map<String, List<Map<String, Object>>>) parsedYaml;
        } else {
            throw new ManifestParsingException("Could not parse the manifest file into a map: " + manifestFile.getPath());
        }

        List<Map<String, Object>> applicationList = parsedYamlMap.get("applications");
        if (applicationList == null) {
            throw new ManifestParsingException("Manifest file does not start with an 'applications' block.");
        } else if (applicationList.size() == 0) {
            throw new ManifestParsingException("The 'applications' block is empty.");
        }
        return applicationList;
    }
}
