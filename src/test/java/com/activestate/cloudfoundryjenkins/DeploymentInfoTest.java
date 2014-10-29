/**
 * Copyright (c) ActiveState 2014 - ALL RIGHTS RESERVED.
 */

package com.activestate.cloudfoundryjenkins;

import com.activestate.cloudfoundryjenkins.CloudFoundryPushPublisher.EnvironmentVariable;
import com.activestate.cloudfoundryjenkins.CloudFoundryPushPublisher.OptionalManifest;
import com.activestate.cloudfoundryjenkins.CloudFoundryPushPublisher.ServiceName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.activestate.cloudfoundryjenkins.CloudFoundryPushPublisher.DescriptorImpl.*;
import static org.junit.Assert.*;

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
        assertEquals(DEFAULT_MEMORY, deploymentInfo.getMemory());
        assertEquals("jenkins-build-name", deploymentInfo.getHostname());
        assertEquals(DEFAULT_INSTANCES, deploymentInfo.getInstances());
        assertEquals(DEFAULT_TIMEOUT, deploymentInfo.getTimeout());
        assertEquals(false, deploymentInfo.isNoRoute());
        assertEquals("domain-name", deploymentInfo.getDomain());
        assertEquals(".", deploymentInfo.getAppPath());
        assertNull(deploymentInfo.getBuildpack());
        assertNull(deploymentInfo.getCommand());

        assertTrue(deploymentInfo.getEnvVars().isEmpty());
        assertTrue(deploymentInfo.getServicesNames().isEmpty());
    }


    @Test
    public void testOptionalJenkinsConfigAllOptions() throws Exception {
        List<EnvironmentVariable> envVars = new ArrayList<EnvironmentVariable>();
        envVars.add(new EnvironmentVariable("ENV_VAR_ONE", "value1"));
        envVars.add(new EnvironmentVariable("ENV_VAR_TWO", "value2"));
        envVars.add(new EnvironmentVariable("ENV_VAR_THREE", "value3"));
        List<ServiceName> services = new ArrayList<ServiceName>();
        services.add(new ServiceName("service_name_one"));
        services.add(new ServiceName("service_name_two"));
        services.add(new ServiceName("service_name_three"));
        OptionalManifest jenkinsManifest =
                new OptionalManifest("hello-java", 512, "testhost", 4, 42, true,
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
        OptionalManifest jenkinsManifest =
                new OptionalManifest("", 0, "", 0, 0, false, "", "", "", "", null, null);
        DeploymentInfo deploymentInfo =
                new DeploymentInfo(System.out, null, jenkinsManifest, "jenkins-build-name", "domain-name");

        assertEquals("jenkins-build-name", deploymentInfo.getAppName());
        assertEquals(DEFAULT_MEMORY, deploymentInfo.getMemory());
        assertEquals("jenkins-build-name", deploymentInfo.getHostname());
        assertEquals(DEFAULT_INSTANCES, deploymentInfo.getInstances());
        assertEquals(DEFAULT_TIMEOUT, deploymentInfo.getTimeout());
        assertEquals(false, deploymentInfo.isNoRoute());
        assertEquals("domain-name", deploymentInfo.getDomain());
        assertEquals(".", deploymentInfo.getAppPath());
        assertNull(deploymentInfo.getBuildpack());
        assertNull(deploymentInfo.getCommand());

        assertTrue(deploymentInfo.getEnvVars().isEmpty());
        assertTrue(deploymentInfo.getServicesNames().isEmpty());
    }
}
