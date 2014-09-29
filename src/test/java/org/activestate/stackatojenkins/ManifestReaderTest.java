package org.activestate.stackatojenkins;

import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ManifestReaderTest {

    @Test
    public void testGetApplicationInfo() throws Exception {
        File manifest = new File(getClass().getResource("hello-java-manifest.yml").toURI());
        ManifestReader reader = new ManifestReader(manifest);
        Map<String, Object> result = reader.getApplicationInfo();
        assertEquals(result.get("name"), "hello-java");
        assertEquals(result.get("memory"), "512M");
        assertEquals(result.get("path"), "target/hello-java-1.0.war");
    }
}
