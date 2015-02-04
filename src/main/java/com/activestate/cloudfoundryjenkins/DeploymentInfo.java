/**
 * Copyright (c) ActiveState 2014 - ALL RIGHTS RESERVED.
 */

package com.activestate.cloudfoundryjenkins;

import com.activestate.cloudfoundryjenkins.CloudFoundryPushPublisher.DescriptorImpl;
import com.activestate.cloudfoundryjenkins.CloudFoundryPushPublisher.EnvironmentVariable;
import com.activestate.cloudfoundryjenkins.CloudFoundryPushPublisher.ManifestChoice;
import com.activestate.cloudfoundryjenkins.CloudFoundryPushPublisher.ServiceName;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains all deployment info of a single application.
 * The class is in charge of default values, and of reading from the Jenkins config if needed.
 */
public class DeploymentInfo {

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

    /**
     * Constructor for reading the manifest.yml file.
     * Takes an appInfo Map that is created from a ManifestReader.
     */
    public DeploymentInfo(PrintStream logger, Map<String, Object> appInfo, String jenkinsBuildName,
                          String defaultDomain) throws IOException, ManifestParsingException, InterruptedException {

        readManifestFile(logger, appInfo, jenkinsBuildName, defaultDomain);
    }

    /**
     * Constructor for reading the optional Jenkins config.
     */
    public DeploymentInfo(PrintStream logger, ManifestChoice optionalJenkinsConfig,
                          String jenkinsBuildName, String defaultDomain)
            throws IOException, ManifestParsingException, InterruptedException {

        readOptionalJenkinsConfig(logger, optionalJenkinsConfig, jenkinsBuildName, defaultDomain);
    }

    private void readManifestFile(PrintStream logger, Map<String, Object> manifestJson,
                                  String jenkinsBuildName, String defaultDomain) {

        // Important optional attributes, we should warn in case they are missing
        if (manifestJson == null) {
            manifestJson = new HashMap<String, Object>();
        }

        appName = (String) manifestJson.get("name");
        if (appName == null) {
            logger.println("WARNING: No application name. Using Jenkins build name: " + jenkinsBuildName);
            appName = jenkinsBuildName;
        }

        int memory = 0;
        String memString = (String) manifestJson.get("memory");
        if (memString == null) {
            logger.println("WARNING: No manifest value for memory. Using default value: " +
                    DescriptorImpl.DEFAULT_MEMORY);
            memory = DescriptorImpl.DEFAULT_MEMORY;
        } else if (memString.toLowerCase().endsWith("m")) {
            memory = Integer.parseInt(memString.substring(0, memString.length() - 1));
        }
        this.memory = memory;

        hostname = (String) manifestJson.get("host");
        if (hostname == null) {
            logger.println("WARNING: No manifest value for hostname. Using app name: " + appName);
            hostname = appName;
        }

        // Non-important optional attributes, no need to warn
        Integer instances = (Integer) manifestJson.get("instances");
        if (instances == null) {
            instances = DescriptorImpl.DEFAULT_INSTANCES;
        }
        this.instances = instances;

        Integer timeout = (Integer) manifestJson.get("timeout");
        if (timeout == null) {
            timeout = DescriptorImpl.DEFAULT_TIMEOUT;
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
            if (envVarsSuppressed != null) {
                this.envVars = envVarsSuppressed;
            }
        } catch (ClassCastException e) {
            logger.println("WARNING: Could not parse env vars into a map. Ignoring env vars.");
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> servicesSuppressed = (List<String>) manifestJson.get("services");
            if (servicesSuppressed != null) {
                this.servicesNames = servicesSuppressed;
            }
        } catch (ClassCastException e) {
            logger.println("WARNING: Could not parse services into a list. Ignoring services.");
        }
    }

    private void readOptionalJenkinsConfig(PrintStream logger, ManifestChoice jenkinsConfig,
                                           String jenkinsBuildName, String defaultDomain) {

        this.appName = jenkinsConfig.appName;
        if (appName.equals("")) {
            logger.println("WARNING: No application name. Using Jenkins build name: " + jenkinsBuildName);
            appName = jenkinsBuildName;
        }
        this.memory = jenkinsConfig.memory;
        if (memory == 0) {
            logger.println("WARNING: Missing value for memory. Using default value: " + DescriptorImpl.DEFAULT_MEMORY);
            memory = DescriptorImpl.DEFAULT_MEMORY;
        }
        this.hostname = jenkinsConfig.hostname;
        if (hostname.equals("")) {
            logger.println("WARNING: Missing value for hostname. Using app name: " + appName);
            hostname = appName;
        }

        this.instances = jenkinsConfig.instances;
        if (instances == 0) {
            instances = DescriptorImpl.DEFAULT_INSTANCES;
        }

        this.timeout = jenkinsConfig.timeout;
        if (timeout == 0) {
            timeout = DescriptorImpl.DEFAULT_TIMEOUT;
        }

        // noRoute's default value is already false, which is acceptable
        this.noRoute = jenkinsConfig.noRoute;

        this.domain = jenkinsConfig.domain;
        if (domain.equals("")) {
            domain = defaultDomain;
        }

        // These must be null, not just empty string
        this.buildpack = jenkinsConfig.buildpack;
        if (buildpack.equals("")) {
            buildpack = null;
        }
        this.command = jenkinsConfig.command;
        if (command.equals("")) {
            command = null;
        }
        this.appPath = jenkinsConfig.appPath;
        if (appPath.equals("")) {
            appPath = ".";
        }

        List<EnvironmentVariable> manifestEnvVars = jenkinsConfig.envVars;
        if (manifestEnvVars != null) {
            for (EnvironmentVariable var : manifestEnvVars) {
                this.envVars.put(var.key, var.value);
            }
        }

        List<ServiceName> manifestServicesNames = jenkinsConfig.servicesNames;
        if (manifestServicesNames != null) {
            for (ServiceName service : manifestServicesNames) {
                this.servicesNames.add(service.name);
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
