package org.activestate.stackatojenkins;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.activestate.stackatojenkins.StackatoPushPublisher.OptionalManifest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentInfo {

    private static final String DEFAULT_MANIFEST_PATH = "manifest.yml";

    private static final Integer DEFAULT_MEMORY = 512;
    private static final Integer DEFAULT_INSTANCES = 1;
    private static final Integer DEFAULT_TIMEOUT = 60;

    private String appName;
    private int memory;
    private String hostname;
    private int instances;
    private int timeout;
    private boolean noRoute;
    private String appPath;
    private String buildpack;
    private String command;
    private String domain;

    private Map<String, String> envVars = new HashMap<String, String>();
    private List<String> servicesNames = new ArrayList<String>();

    public DeploymentInfo(AbstractBuild build, BuildListener listener, OptionalManifest optionalManifest,
                          String jenkinsBuildName, String defaultDomain)
            throws IOException, ManifestParsingException, InterruptedException {

        if (optionalManifest == null) {
            // Read manifest.yml
            FilePath appFilePath = new FilePath(build.getWorkspace(), DEFAULT_MANIFEST_PATH);
            File appFile = new File(appFilePath.toURI());
            ManifestReader manifestReader = new ManifestReader(appFile);
            Map<String, Object> applicationInfo = manifestReader.getApplicationInfo(null);
            listener.getLogger().println(applicationInfo.toString());
            readManifestFile(listener, applicationInfo, jenkinsBuildName, defaultDomain);
        } else {
            // Read Jenkins configuration
            readOptionalJenkinsConfig(listener, optionalManifest, jenkinsBuildName, defaultDomain);
        }
    }

    private void readManifestFile(BuildListener listener, Map<String, Object> manifestJson,
                                  String jenkinsBuildName, String defaultDomain) {

        // Important optional attributes, we should warn in case they are missing

        appName = (String) manifestJson.get("name");
        if (appName == null) {
            listener.getLogger().
                    println("WARNING: No application name. Using Jenkins build name: " + jenkinsBuildName);
            appName = jenkinsBuildName;
        }

        int memory = 0;
        String memString = (String) manifestJson.get("memory");
        if (memString == null) {
            listener.getLogger().
                    println("WARNING: No manifest value for memory. Using default value: " + DEFAULT_MEMORY);
            memory = DEFAULT_MEMORY;
        } else if (memString.toLowerCase().endsWith("m")) {
            memory = Integer.parseInt(memString.substring(0, memString.length() - 1));
        }
        this.memory = memory;

        hostname = (String) manifestJson.get("host");
        if (hostname == null) {
            listener.getLogger().println("WARNING: No manifest value for hostname. Using app name: " + appName);
            hostname = appName;
        }

        // Non-important optional attributes, no need to warn

        Integer instances = (Integer) manifestJson.get("instances");
        if (instances == null) {
            instances = DEFAULT_INSTANCES;
        }
        this.instances = instances;

        Integer timeout = (Integer) manifestJson.get("timeout");
        if (timeout == null) {
            timeout = DEFAULT_TIMEOUT;
        }
        this.timeout = timeout;

        Boolean noRoute = (Boolean) manifestJson.get("no-route");
        if (noRoute == null) {
            noRoute = false;
        }
        this.noRoute = noRoute;

        String domain = (String) manifestJson.get("domain");
        if (domain == null) {
            domain = defaultDomain;
        }
        this.domain = domain;

        String appPath = (String) manifestJson.get("path");
        if (appPath == null) {
            appPath = ".";
        }
        this.appPath = appPath;

        // Optional attributes with no defaults, it's ok if those are null
        this.buildpack = (String) manifestJson.get("buildpack");
        this.command = (String) manifestJson.get("command");

        // Env vars and services
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> envVarsSuppressed = (Map<String, String>) manifestJson.get("env");
            this.envVars = envVarsSuppressed;
        } catch (ClassCastException e) {
            listener.getLogger().println("WARNING: Could not parse env vars into a map. Ignoring env vars.");
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> servicesSuppressed = (List<String>) manifestJson.get("services");
            this.servicesNames = servicesSuppressed;
        } catch (ClassCastException e) {
            listener.getLogger().println("WARNING: Could not parse services into a list. Ignoring services.");
        }
    }

    private void readOptionalJenkinsConfig(BuildListener listener, OptionalManifest optionalManifest,
                                           String jenkinsBuildName, String defaultDomain) {

        this.appName = optionalManifest.appName;
        if (appName.equals("")) {
            listener.getLogger().
                    println("WARNING: No application name. Using Jenkins build name: " + jenkinsBuildName);
            appName = jenkinsBuildName;
        }
        this.memory = optionalManifest.memory;
        if (memory == 0) {
            listener.getLogger().
                    println("WARNING: Missing value for memory. Using default value: " + DEFAULT_MEMORY);
            memory = DEFAULT_MEMORY;
        }
        this.hostname = optionalManifest.hostname;
        if (hostname.equals("")) {
            listener.getLogger().println("WARNING: Missing value for hostname. Using app name: " + appName);
            hostname = appName;
        }

        this.instances = optionalManifest.instances;
        if (instances == 0) {
            instances = DEFAULT_INSTANCES;
        }

        this.timeout = optionalManifest.timeout;
        if (timeout == 0) {
            timeout = DEFAULT_TIMEOUT;
        }

        // noRoute's default value is already false, which is acceptable
        this.noRoute = optionalManifest.noRoute;

        this.domain = optionalManifest.domain;
        if (domain.equals("")) {
            domain = defaultDomain;
        }

        // These must be null, not just empty string
        this.buildpack = optionalManifest.buildpack;
        if (buildpack.equals("")) {
            buildpack = null;
        }
        this.command = optionalManifest.command;
        if (command.equals("")) {
            command = null;
        }
        this.appPath = optionalManifest.appPath;
        if (appPath.equals("")) {
            appPath = ".";
        }

        String manifestEnvVars = optionalManifest.envVars;
        if (!manifestEnvVars.isEmpty()) {
            String[] individualVars = manifestEnvVars.split("\n");
            for (String var : individualVars) {
                String[] split = var.split(":");
                if (split.length >= 2) {
                    this.envVars.put(split[0].trim(), split[1].trim());
                } else {
                    listener.getLogger().println("WARNING: Malformed env vars settings. Ignoring.");
                }
            }
        }

        String servicesNames = optionalManifest.servicesNames;
        if (!servicesNames.isEmpty()) {
            String[] individualServices = servicesNames.split(",");
            for (String service : individualServices) {
                this.servicesNames.add(service.trim());
            }
        }
    }

    public String getAppName() {
        return appName;
    }

    public int getMemory() {
        return memory;
    }

    public String getHostname() {
        return hostname;
    }

    public int getInstances() {
        return instances;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isNoRoute() {
        return noRoute;
    }

    public String getAppPath() {
        return appPath;
    }

    public String getBuildpack() {
        return buildpack;
    }

    public String getCommand() {
        return command;
    }

    public String getDomain() {
        return domain;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public List<String> getServicesNames() {
        return servicesNames;
    }

}
