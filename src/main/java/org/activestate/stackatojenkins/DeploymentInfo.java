package org.activestate.stackatojenkins;

import hudson.model.BuildListener;
import org.activestate.stackatojenkins.StackatoPushPublisher.OptionalManifest;

import java.io.FileNotFoundException;
import java.util.Map;

public class DeploymentInfo {

    public static final Integer DEFAULT_MEMORY = 512;
    public static final Integer DEFAULT_INSTANCES = 1;
    public static final Integer DEFAULT_TIMEOUT = 60;

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

    public DeploymentInfo(BuildListener listener, OptionalManifest optionalManifest,
                          String jenkinsBuildName, String defaultDomain)
            throws FileNotFoundException, ManifestParsingException {

        // Read manifest.yml
        if (optionalManifest == null) {
            ManifestReader manifestReader = new ManifestReader();
            Map<String, Object> applicationInfo = manifestReader.getApplicationInfo(null);

            // Important optional attributes, we should warn in case they are missing.

            appName = (String) applicationInfo.get("appName");
            if (appName == null) {
                listener.getLogger().
                        println("WARNING: No application name. Using Jenkins build name: " + jenkinsBuildName);
                appName = jenkinsBuildName;
            }

            Integer memory = (Integer) applicationInfo.get("memory");
            if (memory == null) {
                listener.getLogger().
                        println("WARNING: No manifest value for memory. Using default value: " + DEFAULT_MEMORY);
                memory = DEFAULT_MEMORY;
            }
            this.memory = memory;

            hostname = (String) applicationInfo.get("host");
            if (hostname == null) {
                listener.getLogger().println("WARNING: No manifest value for hostname. Using app name: " + appName);
                hostname = appName;
            }

            // Non-important optional attributes, no need to warn.

            Integer instances = (Integer) applicationInfo.get("instances");
            if (instances == null) {
                instances = DEFAULT_INSTANCES;
            }
            this.instances = instances;

            Integer timeout = (Integer) applicationInfo.get("timeout");
            if (timeout == null) {
                timeout = DEFAULT_TIMEOUT;
            }
            this.timeout = timeout;

            Boolean noRoute = (Boolean) applicationInfo.get("no-route");
            if (noRoute == null) {
                noRoute = false;
            }
            this.noRoute = noRoute;

            String domain = (String) applicationInfo.get("domain");
            if (domain == null) {
                domain = defaultDomain;
            }
            this.domain = domain;

            // Optional attributes with no defaults, it's ok if those are null.

            this.buildpack = (String) applicationInfo.get("buildpack");
            this.command = (String) applicationInfo.get("command");
            this.appPath = (String) applicationInfo.get("path");
        }
        // Read Jenkins configuration
        else {
            this.appName = optionalManifest.appName;
            this.memory = optionalManifest.memory;
            this.hostname = optionalManifest.hostname;
            this.instances = optionalManifest.instances;
            this.timeout = optionalManifest.timeout;
            this.noRoute = optionalManifest.noRoute;
            this.buildpack = optionalManifest.buildpack;
            this.command = optionalManifest.command;
            this.domain = optionalManifest.domain;
            this.appPath = optionalManifest.appPath;
        }

        // TODO: env vars and services
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
}
