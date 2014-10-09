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

public class DeploymentInfoTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testReadManifestFileAllOptions() throws Exception {
        File manifestFile = new File(getClass().getResource("all-options-manifest.yml").toURI());
        DeploymentInfo deploymentInfo =
                new DeploymentInfo(System.out, manifestFile, null, "jenkins-build-name", "domain-name");

        assertEquals(deploymentInfo.getAppName(), "hello-java");
        assertEquals(deploymentInfo.getMemory(), 512);
        assertEquals(deploymentInfo.getHostname(), "testhost");
        assertEquals(deploymentInfo.getInstances(), 4);
        assertEquals(deploymentInfo.getTimeout(), 42);
        assertEquals(deploymentInfo.isNoRoute(), true);
        assertEquals(deploymentInfo.getDomain(), "testdomain.local");
        assertEquals(deploymentInfo.getAppPath(), "target/hello-java-1.0.war");
        assertEquals(deploymentInfo.getBuildpack(), "https://github.com/heroku/heroku-buildpack-hello");
        assertEquals(deploymentInfo.getCommand(), "echo Hello");

        Map<String, String> expectedEnvs = new HashMap<String, String>();
        expectedEnvs.put("ENV_VAR_ONE", "value1");
        expectedEnvs.put("ENV_VAR_TWO", "value2");
        expectedEnvs.put("ENV_VAR_THREE", "value3");
        assertEquals(deploymentInfo.getEnvVars(), expectedEnvs);

        List<String> expectedServices = new ArrayList<String>();
        expectedServices.add("service_name_one");
        expectedServices.add("service_name_two");
        expectedServices.add("service_name_three");
        assertEquals(deploymentInfo.getServicesNames(), expectedServices);
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

        assertEquals(deploymentInfo.getAppName(), "hello-java");
        assertEquals(deploymentInfo.getMemory(), 512);
        assertEquals(deploymentInfo.getHostname(), "testhost");
        assertEquals(deploymentInfo.getInstances(), 4);
        assertEquals(deploymentInfo.getTimeout(), 42);
        assertEquals(deploymentInfo.isNoRoute(), true);
        assertEquals(deploymentInfo.getDomain(), "testdomain.local");
        assertEquals(deploymentInfo.getAppPath(), "target/hello-java-1.0.war");
        assertEquals(deploymentInfo.getBuildpack(), "https://github.com/heroku/heroku-buildpack-hello");
        assertEquals(deploymentInfo.getCommand(), "echo Hello");

        Map<String, String> expectedEnvs = new HashMap<String, String>();
        expectedEnvs.put("ENV_VAR_ONE", "value1");
        expectedEnvs.put("ENV_VAR_TWO", "value2");
        expectedEnvs.put("ENV_VAR_THREE", "value3");
        assertEquals(deploymentInfo.getEnvVars(), expectedEnvs);

        List<String> expectedServices = new ArrayList<String>();
        expectedServices.add("service_name_one");
        expectedServices.add("service_name_two");
        expectedServices.add("service_name_three");
        assertEquals(deploymentInfo.getServicesNames(), expectedServices);
    }
}
