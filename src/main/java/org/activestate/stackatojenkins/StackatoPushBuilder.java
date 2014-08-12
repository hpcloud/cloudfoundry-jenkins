package org.activestate.stackatojenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.Staging;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StackatoPushBuilder extends Builder {

    public final String target;
    public final String organization;
    public final String cloudSpace;
    public final String username;
    public final String password;
    public final String uri;

    @DataBoundConstructor
    public StackatoPushBuilder(String target, String organization, String cloudSpace,
                               String username, String password, String uri) {
        this.target = target;
        this.organization = organization;
        this.cloudSpace = cloudSpace;
        this.username = username;
        this.password = password;
        this.uri = uri;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

        try {
            String appName = build.getProject().getDisplayName();

            String fullTarget = target;
            if (!fullTarget.startsWith("https://")) {
                if (!fullTarget.startsWith("api.")) {
                    fullTarget = "https://api." + fullTarget;
                } else {
                    fullTarget = "https://" + fullTarget;
                }
            }
            URL targetUrl = new URL(fullTarget);

            listener.getLogger().println("Logging to stackato with:" + username + "/" + password);
            listener.getLogger().println("Target URL:" + targetUrl.getHost());
            listener.getLogger().println("Org:" + organization);
            listener.getLogger().println("Space:" + cloudSpace);

            CloudCredentials credentials = new CloudCredentials(username, password);
            CloudFoundryClient client = new CloudFoundryClient(credentials, targetUrl, organization, cloudSpace);
            client.login();

            listener.getLogger().println("Pushing " + appName + " app to " + fullTarget);

            listener.getLogger().println("Creating new app.");
            Staging staging = new Staging();
            List<String> uris = new ArrayList<String>();
            uris.add(uri);
            List<String> services = new ArrayList<String>();
            client.createApplication(appName, staging, 512, uris, services);

            listener.getLogger().println("Pushing app bits.");
            FilePath warPath = new FilePath(build.getWorkspace(), "target/hello-java-1.0.war");
            File warFile = new File(warPath.toURI());
            client.uploadApplication(appName, warFile);

            //TODO: start app and show url

            listener.getLogger().println("Stackato push successful.");
            return true;
        } catch (MalformedURLException e) {
            listener.getLogger().println("The target URL is not valid: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
            return false;
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Push to Stackato";
        }

    }
}
