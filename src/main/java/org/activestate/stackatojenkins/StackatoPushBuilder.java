package org.activestate.stackatojenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.Staging;
import org.kohsuke.stapler.DataBoundConstructor;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class StackatoPushBuilder extends Builder {

    private final String target;
    private final String organization;
    private final String cloudSpace;
    private final String username;
    private final String password;
    private final String uri;

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
            URL targetUrl = new URL(target);
            CloudCredentials credentials = new CloudCredentials(username, password);
            CloudFoundryClient client = new CloudFoundryClient(credentials, targetUrl, organization, cloudSpace);
            return true;
        } catch (MalformedURLException e) {
            listener.getLogger().println("The target URL is not valid: " + e.getMessage());
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
