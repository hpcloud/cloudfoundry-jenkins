package org.activestate.stackatojenkins;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StackatoPushPublisherTest {

    private static final String TEST_TARGET = System.getProperty("target");
    private static final String TEST_USERNAME = System.getProperty("username");
    private static final String TEST_PASSWORD = System.getProperty("password");
    private static final String TEST_ORG = System.getProperty("org");
    private static final String TEST_SPACE = System.getProperty("space");

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void deleteAllApps() throws MalformedURLException {
        String fullTarget = TEST_TARGET;
        if (!fullTarget.startsWith("https://")) {
            if (!fullTarget.startsWith("api.")) {
                fullTarget = "https://api." + fullTarget;
            } else {
                fullTarget = "https://" + fullTarget;
            }
        }
        URL targetUrl = new URL(fullTarget);

        CloudCredentials credentials = new CloudCredentials(TEST_USERNAME, TEST_PASSWORD);
        CloudFoundryClient client = new CloudFoundryClient(credentials, targetUrl, TEST_ORG, TEST_SPACE);
        client.login();
        client.deleteAllApplications();
    }

    @Test
    public void testPerformSimplePushManifestFile() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        StackatoPushPublisher stackato =
                new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE, TEST_USERNAME, TEST_PASSWORD, null);
        project.getPublishersList().add(stackato);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        String log = FileUtils.readFileToString(build.getLogFile());
        System.out.println(log);

        assertTrue("Build did not succeed", build.getResult().isBetterOrEqualTo(Result.SUCCESS));
        assertTrue("Build did not display staging logs", log.contains("Downloaded app package"));

        System.out.println("stackato.getAppURI() = " + stackato.getAppURI());
        String uri = stackato.getAppURI();
        Request request = Request.Get(uri);
        HttpResponse response = request.execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        assertEquals("Get request did not respond 200 OK", 200, statusCode);
        String content = EntityUtils.toString(response.getEntity());
        System.out.println(content);
        assertTrue("App did not send back correct text", content.contains("Hello from"));
    }

    @Test
    public void testPerformSimplePushOptionalManifest() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        StackatoPushPublisher.OptionalManifest manifest =
                new StackatoPushPublisher.OptionalManifest("hello-java", 512, "", 0, 0, false,
                        "target/hello-java-1.0.war", "", "", "");
        StackatoPushPublisher stackato =
                new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE, TEST_USERNAME, TEST_PASSWORD, manifest);
        project.getPublishersList().add(stackato);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        String log = FileUtils.readFileToString(build.getLogFile());
        System.out.println(log);

        assertTrue("Build did not succeed", build.getResult().isBetterOrEqualTo(Result.SUCCESS));
        assertTrue("Build did not display staging logs", log.contains("Downloaded app package"));

        System.out.println("stackato.getAppURI() = " + stackato.getAppURI());
        String uri = stackato.getAppURI();
        Request request = Request.Get(uri);
        HttpResponse response = request.execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        assertEquals("Get request did not respond 200 OK", 200, statusCode);
        String content = EntityUtils.toString(response.getEntity());
        System.out.println(content);
        assertTrue("App did not send back correct text", content.contains("Hello from"));
    }

    @Test
    public void testPerformCustomBuildpack() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("heroku-node-js-sample.zip")));
        StackatoPushPublisher.OptionalManifest manifest =
                new StackatoPushPublisher.OptionalManifest("heroku-node-js-sample", 512, "", 1, 60, false,
                        ".",
                        "https://github.com/heroku/heroku-buildpack-nodejs", "", "");
        StackatoPushPublisher stackato =
                new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE, TEST_USERNAME, TEST_PASSWORD, manifest);
        project.getPublishersList().add(stackato);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        String log = FileUtils.readFileToString(build.getLogFile());
        System.out.println(log);

        assertTrue("Build did not succeed", build.getResult().isBetterOrEqualTo(Result.SUCCESS));
        assertTrue("Build did not display staging logs", log.contains("Downloading and installing node"));

        System.out.println("stackato.getAppURI() = " + stackato.getAppURI());
        String uri = stackato.getAppURI();
        Request request = Request.Get(uri);
        HttpResponse response = request.execute().returnResponse();
        int statusCode = response.getStatusLine().getStatusCode();
        assertEquals("Get request did not respond 200 OK", 200, statusCode);
        String content = EntityUtils.toString(response.getEntity());
        System.out.println(content);
        assertTrue("App did not send back correct text", content.contains("Hello World!"));
    }

    @Test
    public void testPerformUnknownHost() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        StackatoPushPublisher stackato = new StackatoPushPublisher("https://does-not-exist.local", TEST_ORG, TEST_SPACE,
                TEST_USERNAME, TEST_PASSWORD, null);
        project.getPublishersList().add(stackato);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");
        assertTrue("Build succeeded where it should have failed", build.getResult().isWorseOrEqualTo(Result.FAILURE));

        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);
        assertTrue("Build did not write error message", s.contains("ERROR: Unknown host"));
    }

    @Test
    public void testPerformWrongCredentials() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                "NotAdmin", "BadPassword", null);
        project.getPublishersList().add(stackato);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");
        assertTrue("Build succeeded where it should have failed", build.getResult().isWorseOrEqualTo(Result.FAILURE));

        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);
        assertTrue("Build did not write error message", s.contains("ERROR: Wrong username or password"));
    }
}
