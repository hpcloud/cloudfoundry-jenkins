package org.activestate.stackatojenkins;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ManifestReaderTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testGetApplicationInfo() throws Exception {
        File manifest = new File(getClass().getResource("hello-java-manifest.yml").toURI());
        ManifestReader reader = new ManifestReader(manifest);
        Map<String, Object> result = reader.getApplicationInfo();
        assertEquals(result.get("name"), "hello-java");
        assertEquals(result.get("memory"), "512M");
        assertEquals(result.get("path"), "target/hello-java-1.0.war");
    }

    @Test
    public void testGetApplicationInfoMalformedYML() throws Exception {
        exception.expect(ManifestParsingException.class);
        exception.expectMessage("Malformed YAML file");
        File manifest = new File(getClass().getResource("malformed-manifest.yml").toURI());
        ManifestReader reader = new ManifestReader(manifest);
    }

    @Test
    public void testGetApplicationInfoNotAMap() throws Exception {
        exception.expect(ManifestParsingException.class);
        exception.expectMessage("Could not parse the manifest file into a map");
        File manifest = new File(getClass().getResource("not-a-map-manifest.yml").toURI());
        ManifestReader reader = new ManifestReader(manifest);
    }

    @Test
    public void testGetApplicationInfoNoApplicationBlock() throws Exception {
        exception.expect(ManifestParsingException.class);
        exception.expectMessage("Manifest file does not start with an 'applications' block");
        File manifest = new File(getClass().getResource("no-application-block-manifest.yml").toURI());
        ManifestReader reader = new ManifestReader(manifest);
    }

    @Test
    public void testGetApplicationInfoWrongAppName() throws Exception {
        exception.expect(ManifestParsingException.class);
        exception.expectMessage("Manifest file does not contain an app named goodbye-java");
        File manifest = new File(getClass().getResource("hello-java-manifest.yml").toURI());
        ManifestReader reader = new ManifestReader(manifest);
        reader.getApplicationInfo("goodbye-java");
    }
}
