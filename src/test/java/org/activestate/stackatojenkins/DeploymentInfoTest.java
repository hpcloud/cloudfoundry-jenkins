package org.activestate.stackatojenkins;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DeploymentInfoTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testReadManifestFileAllOptions() throws Exception {
        File manifestFile = new File(getClass().getResource("all-options-manifest.yml").toURI());
        DeploymentInfo deploymentInfo =
                new DeploymentInfo(System.out, manifestFile, null, "jenkins-build-name", "domain-name");

        assertEquals("hello-java", deploymentInfo.getAppName());
        assertEquals(512, deploymentInfo.getMemory());
        assertEquals("testhost", deploymentInfo.getHostname());
        assertEquals(4, deploymentInfo.getInstances());
        assertEquals(42, deploymentInfo.getTimeout());
        assertEquals(true, deploymentInfo.isNoRoute());
        assertEquals("testdomain.local", deploymentInfo.getDomain());
        assertEquals("target/hello-java-1.0.war", deploymentInfo.getAppPath());
        assertEquals("https://github.com/heroku/heroku-buildpack-hello", deploymentInfo.getBuildpack());
        assertEquals("echo Hello", deploymentInfo.getCommand());

        Map<String, String> expectedEnvs = new HashMap<String, String>();
        expectedEnvs.put("ENV_VAR_ONE", "value1");
        expectedEnvs.put("ENV_VAR_TWO", "value2");
        expectedEnvs.put("ENV_VAR_THREE", "value3");
        assertEquals(expectedEnvs, deploymentInfo.getEnvVars());

        List<String> expectedServices = new ArrayList<String>();
        expectedServices.add("service_name_one");
        expectedServices.add("service_name_two");
        expectedServices.add("service_name_three");
        assertEquals(expectedServices, deploymentInfo.getServicesNames());
    }

    @Test
    public void testReadManifestFileDefaultOptions() throws Exception {
        File manifestFile = new File(getClass().getResource("no-options-manifest.yml").toURI());
        DeploymentInfo deploymentInfo =
                new DeploymentInfo(System.out, manifestFile, null, "jenkins-build-name", "domain-name");

        assertEquals("jenkins-build-name", deploymentInfo.getAppName());
        assertEquals(DeploymentInfo.DEFAULT_MEMORY, deploymentInfo.getMemory());
        assertEquals("jenkins-build-name", deploymentInfo.getHostname());
        assertEquals(DeploymentInfo.DEFAULT_INSTANCES, deploymentInfo.getInstances());
        assertEquals(DeploymentInfo.DEFAULT_TIMEOUT, deploymentInfo.getTimeout());
        assertEquals(false, deploymentInfo.isNoRoute());
        assertEquals("domain-name", deploymentInfo.getDomain());
        assertEquals(".", deploymentInfo.getAppPath());
        assertNull(deploymentInfo.getBuildpack());
        assertNull(deploymentInfo.getCommand());

        assertNull(deploymentInfo.getEnvVars());
        assertNull(deploymentInfo.getServicesNames());
    }


    @Test
    public void testOptionalJenkinsConfigAllOptions() throws Exception {
        String envVars = "ENV_VAR_ONE: value1\n" +
                "ENV_VAR_TWO: value2\n" +
                "ENV_VAR_THREE: value3";
        String services = "service_name_one, service_name_two, service_name_three";
        StackatoPushPublisher.OptionalManifest jenkinsManifest =
                new StackatoPushPublisher.OptionalManifest("hello-java", 512, "testhost", 4, 42, true,
                        "target/hello-java-1.0.war",
                        "https://github.com/heroku/heroku-buildpack-hello",
                        "echo Hello", "testdomain.local", envVars, services);
        DeploymentInfo deploymentInfo =
                new DeploymentInfo(System.out, null, jenkinsManifest, "jenkins-build-name", "domain-name");

        assertEquals("hello-java", deploymentInfo.getAppName());
        assertEquals(512, deploymentInfo.getMemory());
        assertEquals("testhost", deploymentInfo.getHostname());
        assertEquals(4, deploymentInfo.getInstances());
        assertEquals(42, deploymentInfo.getTimeout());
        assertEquals(true, deploymentInfo.isNoRoute());
        assertEquals("testdomain.local", deploymentInfo.getDomain());
        assertEquals("target/hello-java-1.0.war", deploymentInfo.getAppPath());
        assertEquals("https://github.com/heroku/heroku-buildpack-hello", deploymentInfo.getBuildpack());
        assertEquals("echo Hello", deploymentInfo.getCommand());

        Map<String, String> expectedEnvs = new HashMap<String, String>();
        expectedEnvs.put("ENV_VAR_ONE", "value1");
        expectedEnvs.put("ENV_VAR_TWO", "value2");
        expectedEnvs.put("ENV_VAR_THREE", "value3");
        assertEquals(expectedEnvs, deploymentInfo.getEnvVars());

        List<String> expectedServices = new ArrayList<String>();
        expectedServices.add("service_name_one");
        expectedServices.add("service_name_two");
        expectedServices.add("service_name_three");
        assertEquals(expectedServices, deploymentInfo.getServicesNames());
    }

    @Test
    public void testReadJenkinsConfigDefaultOptions() throws Exception {
        StackatoPushPublisher.OptionalManifest jenkinsManifest =
                new StackatoPushPublisher.OptionalManifest("", 0, "", 0, 0, false, "", "", "", "", "", "");
        DeploymentInfo deploymentInfo =
                new DeploymentInfo(System.out, null, jenkinsManifest, "jenkins-build-name", "domain-name");

        assertEquals("jenkins-build-name", deploymentInfo.getAppName());
        assertEquals(DeploymentInfo.DEFAULT_MEMORY, deploymentInfo.getMemory());
        assertEquals("jenkins-build-name", deploymentInfo.getHostname());
        assertEquals(DeploymentInfo.DEFAULT_INSTANCES, deploymentInfo.getInstances());
        assertEquals(DeploymentInfo.DEFAULT_TIMEOUT, deploymentInfo.getTimeout());
        assertEquals(false, deploymentInfo.isNoRoute());
        assertEquals("domain-name", deploymentInfo.getDomain());
        assertEquals(".", deploymentInfo.getAppPath());
        assertNull(deploymentInfo.getBuildpack());
        assertNull(deploymentInfo.getCommand());

        assertNull(deploymentInfo.getEnvVars());
        assertNull(deploymentInfo.getServicesNames());
    }
}
