package org.activestate.stackatojenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.*;
import org.cloudfoundry.client.lib.org.springframework.web.client.ResourceAccessException;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StackatoPushPublisher extends Recorder {

    private static final String DEFAULT_MANIFEST_PATH = "manifest.yml";
    private static final int TIMEOUT = 120;

    public final String target;
    public final String organization;
    public final String cloudSpace;
    public final String username;
    public final String password;
    public final OptionalManifest optionalManifest;


    private String appURI;

    @DataBoundConstructor
    public StackatoPushPublisher(String target, String organization, String cloudSpace,
                                 String username, String password,
                                 OptionalManifest optionalManifest) {
        this.target = target;
        this.organization = organization;
        this.cloudSpace = cloudSpace;
        this.username = username;
        this.password = password;
        this.optionalManifest = optionalManifest;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // We don't want to push if the build failed
        if (build.getResult().isWorseThan(Result.SUCCESS))
            return true;

        listener.getLogger().println("target: " + target);
        listener.getLogger().println("organization: " + organization);
        listener.getLogger().println("cloudSpace: " + cloudSpace);
        listener.getLogger().println("username: " + username);
        listener.getLogger().println("password: " + password);
        if (optionalManifest != null) {
            listener.getLogger().println("optionalManifest: " + optionalManifest);
            listener.getLogger().println("optionalManifest.appName: " + optionalManifest.appName);
            listener.getLogger().println("optionalManifest.memory: " + optionalManifest.memory);
            listener.getLogger().println("optionalManifest.hostname: " + optionalManifest.hostname);
            listener.getLogger().println("optionalManifest.instances: " + optionalManifest.instances);
            listener.getLogger().println("optionalManifest.timeout: " + optionalManifest.timeout);
            listener.getLogger().println("optionalManifest.noRoute: " + optionalManifest.noRoute);
            listener.getLogger().println("optionalManifest.appPath: " + optionalManifest.appPath);
            listener.getLogger().println("optionalManifest.buildpack: " + optionalManifest.buildpack);
            listener.getLogger().println("optionalManifest.command: " + optionalManifest.command);
            listener.getLogger().println("optionalManifest.domain: " + optionalManifest.domain);
            listener.getLogger().println("optionalManifest.envVars: " + optionalManifest.envVars);
            for (EnvironmentVariable envVar : optionalManifest.envVars) {
                listener.getLogger().println("envVar.key: " + envVar.key);
                listener.getLogger().println("envVar.value: " + envVar.value);
            }
        } else {
            listener.getLogger().println("optionalManifest is null");
        }

        try {
            String jenkinsBuildName = build.getProject().getDisplayName();

            String fullTarget = target;
            if (!fullTarget.startsWith("https://")) {
                if (!fullTarget.startsWith("api.")) {
                    fullTarget = "https://api." + fullTarget;
                } else {
                    fullTarget = "https://" + fullTarget;
                }
            }
            URL targetUrl = new URL(fullTarget);

            String[] split = fullTarget.split("\\.");
            String domain = split[split.length - 2] + "." + split[split.length - 1];

            FilePath manifestFilePath = new FilePath(build.getWorkspace(), DEFAULT_MANIFEST_PATH);
            File manifestFile = new File(manifestFilePath.toURI());
            DeploymentInfo deploymentInfo =
                    new DeploymentInfo(listener.getLogger(), manifestFile, optionalManifest, jenkinsBuildName, domain);
            String appName = deploymentInfo.getAppName();
            setAppURI("https://" + deploymentInfo.getHostname() + "." + deploymentInfo.getDomain());

            listener.getLogger().println("Logging to stackato with: " + username + "/" + password);
            listener.getLogger().println("Target URL: " + targetUrl.getHost());
            listener.getLogger().println("Org: " + organization);
            listener.getLogger().println("Space: " + cloudSpace);
            listener.getLogger().println("Calculated uri: " + getAppURI());


            CloudCredentials credentials = new CloudCredentials(username, password);
            CloudFoundryClient client = new CloudFoundryClient(credentials, targetUrl, organization, cloudSpace);
            client.login();

            listener.getLogger().println("Pushing " + appName + " app to " + fullTarget);

            // Check if app already exists
            List<CloudApplication> existingApps = client.getApplications();
            boolean alreadyExists = false;
            for (CloudApplication app : existingApps) {
                if (app.getName().equals(appName)) {
                    alreadyExists = true;
                    listener.getLogger().println("App already exists, skipping creation.");
                    break;
                }
            }

            // TODO: Create services
            // List<CloudService> cloudServices = deploymentInfo.getServices();

            // Create app if it doesn't exist
            if (!alreadyExists) {
                listener.getLogger().println("Creating new app.");
                Staging staging = new Staging(deploymentInfo.getCommand(), deploymentInfo.getBuildpack(),
                        null, deploymentInfo.getTimeout());
                List<String> uris = new ArrayList<String>();
                uris.add(getAppURI());
                List<String> services = deploymentInfo.getServicesNames();
                client.createApplication(appName, staging, deploymentInfo.getMemory(), uris, services);
            }

            // Delete route if no-route parameter
            if (deploymentInfo.isNoRoute()) {
                client.deleteRoute(deploymentInfo.getHostname(), deploymentInfo.getDomain());
            }

            // Add environment variables
            if (!deploymentInfo.getEnvVars().isEmpty()) {
                client.updateApplicationEnv(appName, deploymentInfo.getEnvVars());
            }

            // Change number of instances
            if (deploymentInfo.getInstances() > 1) {
                client.updateApplicationInstances(appName, deploymentInfo.getInstances());
            }

            // Push files
            listener.getLogger().println("Pushing app bits.");
            FilePath appPath = new FilePath(build.getWorkspace(), deploymentInfo.getAppPath());
            File appFile = new File(appPath.toURI());
            client.uploadApplication(appName, appFile);

            // Start of restart application
            StartingInfo startingInfo;
            if (!alreadyExists) {
                listener.getLogger().println("Starting application.");
                startingInfo = client.startApplication(appName);
            } else {
                listener.getLogger().println("Restarting application.");
                startingInfo = client.restartApplication(appName);
            }

            // Start printing the staging logs
            // First, try streamLogs()
            try {
                JenkinsApplicationLogListener logListener = new JenkinsApplicationLogListener(listener);
                client.streamLogs(appName, logListener);
            } catch (Exception e) {
                // In case of failure, try getStagingLogs()
                listener.getLogger().println("WARNING: Exception occurred trying to get staging logs via websocket. " +
                        "Switching to alternate method.");
                int offset = 0;
                String stagingLogs = client.getStagingLogs(startingInfo, offset);
                if (stagingLogs == null) {
                    listener.getLogger().println("WARNING: Could not get staging logs with alternate method. " +
                            "Cannot display staging logs.");
                } else {
                    while (stagingLogs != null) {
                        listener.getLogger().println(stagingLogs);
                        offset += stagingLogs.length();
                        stagingLogs = client.getStagingLogs(startingInfo, offset);
                    }
                }
            }

            CloudApplication app = client.getApplication(appName);

            // Keep checking to see if the app is running
            int running = 0;
            int totalInstances = 0;
            for (int tries = 0; tries < TIMEOUT; tries++) {
                running = 0;
                InstancesInfo instancesInfo = client.getApplicationInstances(app);
                if (instancesInfo != null) {
                    List<InstanceInfo> listInstances = instancesInfo.getInstances();
                    totalInstances = listInstances.size();
                    for (InstanceInfo instance : listInstances) {
                        if (instance.getState() == InstanceState.RUNNING) {
                            running++;
                        }
                    }
                    if (running == totalInstances && totalInstances > 0) {
                        break;
                    }
                }
                Thread.sleep(1000);
            }

            String instanceGrammar = "instances";
            if (running == 1)
                instanceGrammar = "instance";
            listener.getLogger().println(running + " " + instanceGrammar + " running out of " + totalInstances);

            if (running > 0) {
                if (running != totalInstances) {
                    listener.getLogger().println("WARNING: Some instances of the application are not running.");
                }
                if (deploymentInfo.isNoRoute()) {
                    listener.getLogger().println("Application is now running. (No route)");
                } else {
                    listener.getLogger().println("Application is now running at " + getAppURI());
                }
                listener.getLogger().println("Stackato push successful.");
                return true;
            } else {
                listener.getLogger().println("ERROR: The application failed to start after " + TIMEOUT + " seconds.");
                listener.getLogger().println("Stackato push failed.");
                return false;
            }

        } catch (MalformedURLException e) {
            listener.getLogger().println("ERROR: The target URL is not valid: " + e.getMessage());
            return false;
        } catch (ResourceAccessException e) {
            listener.getLogger().println("ERROR: Unknown host: " + e.getMessage());
            return false;
        } catch (CloudFoundryException e) {
            if (e.getMessage().equals("403 Access token denied.")) {
                listener.getLogger().println("ERROR: Wrong username or password: " + e.getMessage());
            } else {
                listener.getLogger().println("ERROR: Unknown CloudFoundryException: " + e.getMessage());
            }
            return false;
        } catch (ManifestParsingException e) {
            listener.getLogger().println("ERROR: Could not parse manifest: " + e.getMessage());
            return false;
        } catch (FileNotFoundException e) {
            listener.getLogger().println("ERROR: Could not find manifest file: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getAppURI() {
        return appURI;
    }

    public void setAppURI(String appURI) {
        this.appURI = appURI;
    }

    public static class OptionalManifest {
        public final String appName;
        public final int memory;
        public final String hostname;
        public final int instances;
        public final int timeout;

        public final boolean noRoute;
        public final String appPath;
        public final String buildpack;
        public final String command;
        public final String domain;

        public final List<EnvironmentVariable> envVars;
        public final List<ServiceName> servicesNames;


        @DataBoundConstructor
        public OptionalManifest(String appName, int memory, String hostname, int instances, int timeout,
                                boolean noRoute, String appPath, String buildpack, String command, String domain,
                                List<EnvironmentVariable> envVars, List<ServiceName> servicesNames) {
            this.appName = appName;
            this.memory = memory;
            this.hostname = hostname;
            this.instances = instances;
            this.timeout = timeout;
            this.noRoute = noRoute;
            this.appPath = appPath;
            this.buildpack = buildpack;
            this.command = command;
            this.domain = domain;
            this.envVars = envVars;
            this.servicesNames = servicesNames;
        }
    }

    public static class EnvironmentVariable {

        public final String key;
        public final String value;

        @DataBoundConstructor
        public EnvironmentVariable(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class ServiceName {

        public final String name;

        @DataBoundConstructor
        public ServiceName(String name) {
            this.name = name;
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Push to Stackato";
        }

    }
}
