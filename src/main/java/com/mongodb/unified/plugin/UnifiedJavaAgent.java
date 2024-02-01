package com.mongodb.unified.plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class UnifiedJavaAgent {

    private static volatile File unpackedAgentJar;

    public UnifiedJavaAgent() {
        //NOOP
    }

    public File createTempAgentJar() {
        if (isAgentUnpacked()) {
            return unpackedAgentJar;
        }

        try (InputStream jarStream = GradleModuleTestRunner.class.getClassLoader()
                .getResourceAsStream("instrumentation-agent-1.0-SNAPSHOT-all.jar")) {

            if (jarStream == null) {
                throw new FileNotFoundException("Agent JAR not found");
            }

            File tempFile = Files.createTempFile("unified-agent", ".jar").toFile();
            tempFile.deleteOnExit();

            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                jarStream.transferTo(outputStream);
            }

            unpackedAgentJar = tempFile;
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isAgentUnpacked() {
        return unpackedAgentJar != null && unpackedAgentJar.exists();
    }
}
