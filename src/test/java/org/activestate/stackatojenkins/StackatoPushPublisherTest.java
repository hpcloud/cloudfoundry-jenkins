package org.activestate.stackatojenkins;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.activestate.stackatojenkins.StackatoPushPublisher.EnvironmentVariable;
import org.activestate.stackatojenkins.StackatoPushPublisher.OptionalManifest;
import org.activestate.stackatojenkins.StackatoPushPublisher.ServiceName;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StackatoPushPublisherTest {

    private static final String TEST_TARGET = System.getProperty("target");
    private static final String TEST_USERNAME = System.getProperty("username");
    private static final String TEST_PASSWORD = System.getProperty("password");
    private static final String TEST_ORG = System.getProperty("org");
    private static final String TEST_SPACE = System.getProperty("space");

    private static CloudFoundryClient client;

    @Rule
    public JenkinsRule j = new JenkinsRule();


    @BeforeClass
    public static void initialiseClient() throws MalformedURLException {
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
        client = new CloudFoundryClient(credentials, targetUrl, TEST_ORG, TEST_SPACE);
        client.login();
    }

    @Before
    public void cleanCloudSpace() {
        client.deleteAllApplications();
        client.deleteAllServices();
    }

    @Test
    public void testPerformSimplePushManifestFile() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                TEST_USERNAME, TEST_PASSWORD, false, null);
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
        OptionalManifest manifest =
                new OptionalManifest("hello-java", 512, "", 0, 0, false,
                        "target/hello-java-1.0.war", "", "", "",
                        new ArrayList<EnvironmentVariable>(), new ArrayList<ServiceName>());
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                TEST_USERNAME, TEST_PASSWORD, false, manifest);
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
    public void testPerformMultipleInstances() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        OptionalManifest manifest =
                new OptionalManifest("hello-java", 64, "", 4, 0, false,
                        "target/hello-java-1.0.war", "", "", "",
                        new ArrayList<EnvironmentVariable>(), new ArrayList<ServiceName>());
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                        TEST_USERNAME, TEST_PASSWORD, false, manifest);
        project.getPublishersList().add(stackato);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        String log = FileUtils.readFileToString(build.getLogFile());
        System.out.println(log);

        assertTrue("Build did not succeed", build.getResult().isBetterOrEqualTo(Result.SUCCESS));
        assertTrue("Build did not display staging logs", log.contains("Downloaded app package"));
        assertTrue("Not the correct amount of instances", log.contains("4 instances running out of 4"));

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
        OptionalManifest manifest =
                new OptionalManifest("heroku-node-js-sample", 512, "", 1, 60, false, "",
                        "https://github.com/heroku/heroku-buildpack-nodejs", "", "",
                        new ArrayList<EnvironmentVariable>(), new ArrayList<ServiceName>());
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                        TEST_USERNAME, TEST_PASSWORD, false, manifest);
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
    public void testPerformCustomTimeout() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        OptionalManifest manifest =
                new OptionalManifest("hello-java", 512, "", 0, 1, false,
                        "target/hello-java-1.0.war", "", "", "",
                        new ArrayList<EnvironmentVariable>(), new ArrayList<ServiceName>());
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                TEST_USERNAME, TEST_PASSWORD, false, manifest);
        project.getPublishersList().add(stackato);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        String log = FileUtils.readFileToString(build.getLogFile());
        System.out.println(log);

        assertTrue("Build succeeded where it should have failed", build.getResult().isWorseOrEqualTo(Result.FAILURE));
        assertTrue("Build did not display staging logs", log.contains("Downloaded app package"));
        assertTrue("Build did not display proper error message",
                log.contains("ERROR: The application failed to start after"));
    }

    @Test
    public void testPerformEnvVarsManifestFile() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("python-env.zip")));
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                TEST_USERNAME, TEST_PASSWORD, false, null);
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
        assertTrue("App did not have correct ENV_VAR_ONE", content.contains("ENV_VAR_ONE: value1"));
        assertTrue("App did not have correct ENV_VAR_TWO", content.contains("ENV_VAR_TWO: value2"));
        assertTrue("App did not have correct ENV_VAR_THREE", content.contains("ENV_VAR_THREE: value3"));
    }

    @Test
    public void testPerformServicesNamesManifestFile() throws Exception {
        CloudService service1 = new CloudService();
        service1.setName("mysql_service1");
        service1.setLabel("mysql");
        service1.setPlan("free");
        client.createService(service1);

        CloudService service2 = new CloudService();
        service2.setName("mysql_service2");
        service2.setLabel("mysql");
        service2.setPlan("free");
        client.createService(service2);

        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("python-env-services.zip")));
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                TEST_USERNAME, TEST_PASSWORD, false , null);
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
        assertTrue("App did not have mysql_service1 bound", content.contains("mysql_service1"));
        assertTrue("App did not have mysql_service2 bound", content.contains("mysql_service2"));
    }

    @Test
    public void testPerformNoRoute() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        OptionalManifest manifest =
                new OptionalManifest("hello-java", 512, "", 0, 0, true,
                        "target/hello-java-1.0.war", "", "", "",
                        new ArrayList<EnvironmentVariable>(), new ArrayList<ServiceName>());
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                TEST_USERNAME, TEST_PASSWORD, false, manifest);
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
        assertEquals("Get request did not respond 404 Not Found", 404, statusCode);
    }

    @Test
    public void testPerformUnknownHost() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        StackatoPushPublisher stackato = new StackatoPushPublisher("https://does-not-exist.local", TEST_ORG, TEST_SPACE,
                TEST_USERNAME, TEST_PASSWORD, false, null);
        project.getPublishersList().add(stackato);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        assertTrue("Build succeeded where it should have failed", build.getResult().isWorseOrEqualTo(Result.FAILURE));
        assertTrue("Build did not write error message", s.contains("ERROR: Unknown host"));
    }

    @Test
    public void testPerformWrongCredentials() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(getClass().getResource("hello-java.zip")));
        StackatoPushPublisher stackato = new StackatoPushPublisher(TEST_TARGET, TEST_ORG, TEST_SPACE,
                "NotAdmin", "BadPassword", false, null);
        project.getPublishersList().add(stackato);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName() + " completed");

        String s = FileUtils.readFileToString(build.getLogFile());
        System.out.println(s);

        assertTrue("Build succeeded where it should have failed", build.getResult().isWorseOrEqualTo(Result.FAILURE));
        assertTrue("Build did not write error message", s.contains("ERROR: Wrong username or password"));
    }
}
