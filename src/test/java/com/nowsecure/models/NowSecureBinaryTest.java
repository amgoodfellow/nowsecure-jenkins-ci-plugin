package com.nowsecure.models;

import static org.junit.jupiter.api.Assertions.*;

import hudson.FilePath;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NowSecureBinaryTest {

    private static final String ARCH = System.getProperty("os.arch");
    private static final String OS_NAME = System.getProperty("os.name");

    @CsvSource({"x8664", "amd64", "ia32e", "em64t", "x64"})
    @ParameterizedTest
    void shouldReturnAmd64ForAll64BitArchitectures_linux(String arch) throws Exception {
        var actual = NowSecureBinary.getToolName(arch, "linux");
        assertEquals("ns_linux-amd64", actual, "Not matching");
    }

    @CsvSource({"x8664", "amd64", "ia32e", "em64t", "x64"})
    @ParameterizedTest
    void shouldReturnAmd64ForAll64BitArchitectures_windows(String arch) throws Exception {
        var actual = NowSecureBinary.getToolName(arch, "windows");
        assertEquals("ns_windows-amd64.exe", actual, "Not matching");
    }

    @CsvSource({
        "aarch64,linux",
        "aarch64,windows",
        "arm64,linux",
        "arm64,windows",
    })
    @ParameterizedTest
    void shouldFailWhenGivenInvalidArchitecturePlatformCombo(String arch, String platform) throws Exception {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            NowSecureBinary.getToolName(arch, platform);
        });
        assertTrue(exception.getMessage().contains("Unsupported platform / architecture"));
    }

    @Test
    void shouldTrackTokenIndices() throws Exception {
        var resourceDir = this.getClass().getClassLoader().getResource("./");
        var nsb = new NowSecureBinary(ARCH, OS_NAME, new FilePath(new File(resourceDir.getPath())));
        assertEquals(List.of(), nsb.maskedIndices, "Initial masked indices list should be empty");

        nsb.addArgument("some-argument").addToken("some-token").addArgument("double", "argument");
        assertEquals(List.of(3), nsb.maskedIndices);

        nsb.addToken("new-token");
        assertEquals(List.of(3, 7), nsb.maskedIndices);
        assertEquals(8, nsb.arguments.size());
    }

    @Test
    void shouldAddToolPathToProcessArgumentList() throws Exception {
        var resourceDir = this.getClass().getClassLoader().getResource("./");
        var nsb = new NowSecureBinary(ARCH, OS_NAME, new FilePath(new File(resourceDir.getPath())));
        var toolName = NowSecureBinary.getToolName(ARCH, OS_NAME);

        var constructedToolPath = nsb.toolPath.getRemote();

        assertEquals(
                String.format("%s%s%s", Paths.get(resourceDir.toURI()), File.separator, toolName),
                constructedToolPath,
                "Tool path does not look like it should");
        assertEquals(List.of(constructedToolPath), nsb.arguments, "Tool path not properly added to arguments list");
    }
}
