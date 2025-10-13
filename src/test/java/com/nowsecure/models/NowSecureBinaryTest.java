package com.nowsecure.models;

import static org.junit.Assert.assertThrows;

import hudson.FilePath;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class NowSecureBinaryTests {
    final String arch = System.getProperty("os.arch");
    final String osName = System.getProperty("os.name");

    @CsvSource({"x8664", "amd64", "ia32e", "em64t", "x64"})
    @ParameterizedTest
    void shouldReturnAmd64ForAll64BitArchitectures_linux(String arch) throws Exception {
        var actual = NowSecureBinary.getToolName(arch, "linux");
        Assertions.assertEquals("ns_linux-amd64", actual, "Not matching");
    }

    @CsvSource({"x8664", "amd64", "ia32e", "em64t", "x64"})
    @ParameterizedTest
    void shouldReturnAmd64ForAll64BitArchitectures_windows(String arch) throws Exception {
        var actual = NowSecureBinary.getToolName(arch, "windows");
        Assertions.assertEquals("ns_windows-amd64.exe", actual, "Not matching");
    }

    @CsvSource({
        "aarch64,linux",
        "aarch64,windows",
        "arm64,linux",
        "arm64,windows",
    })
    @ParameterizedTest
    void shouldFailWhenGivenInvalidArchitecutrePlatformCombo(String arch, String platform) throws Exception {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            NowSecureBinary.getToolName(arch, platform);
        });
        Assertions.assertTrue(exception.getMessage().contains("Unsupported platform / architecture"));
    }

    @Test
    void shouldTrackTokenIndeces() throws Exception {
        var resourceDir = this.getClass().getClassLoader().getResource("./");
        var nsb = new NowSecureBinary(arch, osName, new FilePath(new File(resourceDir.getPath())));
        Assertions.assertEquals(List.of(), nsb.maskedIndices, "Initial masked indeces list should be empty");

        nsb.addArgument("some-argument").addToken("some-token").addArgument("double", "argument");
        Assertions.assertEquals(List.of(3), nsb.maskedIndices);

        nsb.addToken("new-token");
        Assertions.assertEquals(List.of(3, 7), nsb.maskedIndices);
        Assertions.assertEquals(nsb.arguments.size(), 8);
    }

    @Test
    void shouldAddToolPathToProcessArgumentList() throws Exception {
        var resourceDir = this.getClass().getClassLoader().getResource("./");
        var nsb = new NowSecureBinary(arch, osName, new FilePath(new File(resourceDir.getPath())));
        var toolName = NowSecureBinary.getToolName(arch, osName);

        var constructedToolPath = nsb.toolPath.getRemote();

        Assertions.assertEquals(
                String.format("%s%s%s", Paths.get(resourceDir.toURI()).toString(), File.separator, toolName),
                constructedToolPath,
                "Tool path does not look like it should");

        Assertions.assertEquals(
                List.of(constructedToolPath), nsb.arguments, "Tool path not properly added to arguments list");
    }
}
