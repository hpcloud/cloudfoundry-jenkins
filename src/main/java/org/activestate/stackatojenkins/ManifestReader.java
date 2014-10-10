package org.activestate.stackatojenkins;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManifestReader {


    private final File manifestFile;
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
        Map<String, Object> result = getApplicationMap(appName);
        if (result == null) {
            result = new HashMap<String, Object>();
        }
        return result;
        // TODO: get global map and combine both global and app map
    }

    /**
     * Alias for getApplicationInfo(String appName) that returns info for the first app
     *
     * @return the deployment info
     */
    public Map<String, Object> getApplicationInfo() throws ManifestParsingException {
        return getApplicationMap(null);
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
    private List<Map<String, Object>> parseManifest()
            throws IOException, ManifestParsingException {
        InputStream inputStream = new FileInputStream(manifestFile);
        Yaml yaml = new Yaml();
        Object parsedYaml;
        try {
            parsedYaml = yaml.load(inputStream);
        } catch (ScannerException e) {
            throw new ManifestParsingException("Malformed YAML file: " + manifestFile.getPath());
        }
        Map<String, List<Map<String, Object>>> parsedYamlMap;
        try {
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> parsedYamlMapSuppressed =
                    (Map<String, List<Map<String, Object>>>) parsedYaml;
            parsedYamlMap = parsedYamlMapSuppressed;
        } catch (ClassCastException e) {
            throw new ManifestParsingException(
                    "Could not parse the manifest file into a map: " + manifestFile.getPath());
        }

        List<Map<String, Object>> applicationList = parsedYamlMap.get("applications");
        if (applicationList == null) {
            throw new ManifestParsingException("Manifest file does not start with an 'applications' block.");
        }
        return applicationList;
    }
}
