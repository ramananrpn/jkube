/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.jkube.kit.common.util.EnvUtil.firstRegistryOf;
import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;
import static org.eclipse.jkube.kit.common.util.EnvUtil.loadTimestamp;
import static org.eclipse.jkube.kit.common.util.EnvUtil.storeTimestamp;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.common.SystemMock;
import org.junit.Ignore;
import org.junit.Test;


public class EnvUtilTest {

    @Test
    public void testConvertTcpToHttpsUrl() {
        // Given
        String urlWithHttpsPort = "tcp://0.0.0.0:2376";
        // When
        String result1 = EnvUtil.convertTcpToHttpUrl(urlWithHttpsPort);
        // Then
        assertThat("https://0.0.0.0:2376").isEqualTo(result1);
    }

    @Test
    public void testConvertTcpToHttpUrl() {
        // Given
        String urlWithHttpPort="tcp://0.0.0.0:2375";
        // When
        String result2 = EnvUtil.convertTcpToHttpUrl(urlWithHttpPort);
        // Then
        assertThat("http://0.0.0.0:2375").isEqualTo(result2);
    }

    @Test
    public void testConvertTcpToHttpUrlShouldDefaultToHttps() {
        // Given
        String url = "tcp://127.0.0.1:32770";
        // When
        String result = EnvUtil.convertTcpToHttpUrl(url);
        // Then
        assertThat("https://127.0.0.1:32770").isEqualTo(result);
    }

    @Test
    public void testExtractLargerVersionWhenBothNull(){
        assertThat(EnvUtil.extractLargerVersion(null,null)).isNull();
    }
    @Test
    public void testExtractLargerVersionWhenBIsNull() {
        //Given
        String versionA = "4.0.2";
        //When
        String result = EnvUtil.extractLargerVersion(versionA,null);
        //Then
        assertThat(versionA).isEqualTo(result);
    }
    @Test
    public void testExtractLargerVersionWhenAIsNull() {
        //Given
        String versionB = "3.1.1.0";
        //When
        String result = EnvUtil.extractLargerVersion(null,versionB);
        //Then
        assertThat(versionB).isEqualTo(result);
    }

    @Test
    public void testExtractLargerVersion() {
        //Given
        //When
        String result = EnvUtil.extractLargerVersion("4.0.0.1","4.0.0");
        //Then
        assertThat("4.0.0.1").isEqualTo(result);
    }

    @Test
    public void testGreaterOrEqualsVersionWhenTrue() {
        //Given
        String versionA = "4.0.2";
        String versionB = "3.1.1.0";
        //When
        boolean result1 = EnvUtil.greaterOrEqualsVersion(versionA,versionB);
        //Then
        assertThat(result1).isTrue();
    }

    @Test
    public void testGreaterOrEqualsVersionWhenEqual() {
        //Given
        String versionA = "4.0.2";
        //When
        boolean result2 = EnvUtil.greaterOrEqualsVersion("4.0.2", versionA);
        //Then
        assertThat(result2).isTrue();
    }


    @Test
    public void testGreaterOrEqualsVersionWhenFalse() {
        //Given
        String versionA = "4.0.2";
        String versionB = "3.1.1.0";
        //When
        boolean result3 = EnvUtil.greaterOrEqualsVersion(versionB,versionA);
        //Then
        assertThat(result3).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore
// TODO: Remove when implementation is fixed
    public void testGreaterOrEqualsVersionCornerCase() {
        //Given
        String versionA = "asdw4.0.2";
        String versionB = "3.1.1.0{0.1}";
        //When
        String result = EnvUtil.extractLargerVersion(versionA, versionB);
        //Then
        fail("Exception should have thrown");
    }

    @Test
    public void testSplitOnLastColonWhenNotNull() {
        // Given
        List<String> list1 = Collections.singletonList("element1:element2");
        // When
        List<String[]> result1 = EnvUtil.splitOnLastColon(list1);
        // Then
        assertThat(1).isEqualTo(result1.size());
        assertThat(2).isEqualTo(result1.get(0).length);
        assertThat(new String[]{"element1", "element2"}).isEqualTo(result1.get(0));
    }

    @Test
    public void testSplitOnLastColonWhenNull() {
        // Given
        List<String> list2 = null;
        // When
        List<String[]> result2 = EnvUtil.splitOnLastColon(list2);
        // Then
        assertThat(result2).isEmpty();
    }

    @Test
    public void testRemoveEmptyEntrieWhenNotNull(){
        //Given
        List<String>  string1 = new ArrayList<>();
        string1.add(" set ");
        string1.add(" set2  ");
        string1.add("");
        //When
        List<String>  result1 = EnvUtil.removeEmptyEntries(string1);
        //Then
        assertThat( new String[]{"set", "set2"}).isEqualTo(result1.toArray());
    }

    @Test
    public void testRemoveEmptyEntriesWhenNull(){
        //Given
        List<String>  string2 = new ArrayList<>();
        string2.add(null);
        //When
        List<String>  result2 = EnvUtil.removeEmptyEntries(string2);
        //Then
        assertThat(result2).isEmpty();
    }


    @Test
    public void testSplitAtCommasAndTrimWhenNotNull(){
        //Given
        Iterable<String>  strings1 = Collections.singleton("hello,world");
        //When
        List<String> result1 = EnvUtil.splitAtCommasAndTrim(strings1);
        //Then
        assertThat(2).isEqualTo(result1.size());
        assertThat("world").isEqualTo(result1.get(1));
    }

    @Test
    public void testSplitAtCommasAndTrimWhenNull(){
        //Given
        Iterable<String>  strings2 = Collections.singleton(null);
        //When
        List<String> result2 = EnvUtil.splitAtCommasAndTrim(strings2);
        //Then
        assertThat(result2).isEmpty();
    }

    @Test
    public void testExtractFromPropertiesAsList() {
        //Given
        String string = "key";
        Properties properties = new Properties();
        properties.put("key.name","value");
        properties.put("key.value","valu");
        properties.put("art","id");
        properties.put("note","bool");
        properties.put("key._combine","bool");
        //When
        List<String> result = EnvUtil.extractFromPropertiesAsList(string,properties);
        //Then
        assertThat(result).isNotNull();
        assertThat(2).isEqualTo(result.size());
        assertThat(new String[]{"valu", "value"}).isEqualTo(result.toArray());
    }

    @Test
    public void testExtractFromPropertiesAsMap(){
        //Given
        String prefix = "key";
        Properties properties = new Properties();
        properties.put("key.name","value");
        properties.put("key.value","valu");
        properties.put("art","id");
        properties.put("note","bool");
        properties.put("key._combine","bool");
        //when
        Map<String, String> result = EnvUtil.extractFromPropertiesAsMap(prefix,properties);
        //Then
        assertThat(result).isNotNull();
        assertThat(2).isEqualTo(result.size());
        assertThat("value").isEqualTo(result.get("name"));
    }

    @Test
    public void testFormatDurationTill() {
        long startTime = System.currentTimeMillis() - 200L;
        assertThat(EnvUtil.formatDurationTill(startTime)).contains("milliseconds");
    }

    @Test
    public void testFormatDurationTillHoursMinutesAndSeconds() {
        long startTime = System.currentTimeMillis() - (60*60*1000 + 60*1000 + 1000);
        String formattedDuration = EnvUtil.formatDurationTill(startTime);
        assertThat(formattedDuration).contains("1 hour, 1 minute and 1 second");
    }

    @Test
    public void testFirstRegistryOf() {
        assertThat("quay.io").isEqualTo(firstRegistryOf("quay.io", "docker.io", "registry.access.redhat.io"));
        assertThat("registry.access.redhat.io").isEqualTo(firstRegistryOf(null, null, "registry.access.redhat.io"));
    }

    @Test
    public void testPrepareAbsolutePath() {
        assumeFalse(isWindows());
        assertThat("test-project/target/testDir/bar").isEqualTo(
                EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "bar").getPath());
        assertThat("/home/redhat/jkube").isEqualTo(
                EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "/home/redhat/jkube").getPath());
    }

    @Test
    public void testPrepareAbsolutePathWindows() {
        assumeTrue(isWindows());
        assertThat("test-project\\target\\testDir\\bar").isEqualTo(
                EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "bar").getPath());
        assertThat("C:\\users\\redhat\\jkube").isEqualTo(
                EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "C:\\users\\redhat\\jkube").getPath());
    }

    @Test
    public void testPrepareAbsoluteSourceDirPath() {
        assumeFalse(isWindows());
        assertThat("test-project/target/testDir").isEqualTo(
                EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "testDir").getPath());
        assertThat("/home/redhat/jkube").isEqualTo(
                EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "/home/redhat/jkube").getPath());
    }

    @Test
    public void testPrepareAbsoluteSourceDirPathWindows() {
        assumeTrue(isWindows());
        assertThat("test-project\\target\\testDir").isEqualTo(
                EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "testDir").getPath());
        assertThat("C:\\users\\redhat\\jkube").isEqualTo(
                EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "C:\\users\\redhat\\jkube").getPath());
    }

    @Test
    public void testStringJoin(){
        //Given
        List<String> list = new ArrayList<>();
        String separator = ",";
        list.add("element1");
        list.add("element2");
        //When
        String result = EnvUtil.stringJoin(list,separator);
        //Then
        assertThat("element1,element2").isEqualTo(result);
    }

    @Test
    public void testExtractMavenPropertyName() {
        assertThat("project.baseDir").isEqualTo(EnvUtil.extractMavenPropertyName("${project.baseDir}"));
        assertThat(EnvUtil.extractMavenPropertyName("roject.notbaseDi")).isNull();
    }

    @Test
    public void testFixupPathWhenNotWindows(){
        //Given
        String test2 = "/etc/ip/";
        //When
        String result2 = EnvUtil.fixupPath(test2);
        //Then
        assertThat("/etc/ip/").isEqualTo(result2);
    }

    @Test
    public void testFixupPathWhenWindows(){
        //Given
        String test1 = "c:\\...\\";
        //When
        String result1 = EnvUtil.fixupPath(test1);
        //Then
        assertThat("/c/.../").isEqualTo(result1);
    }

    @Test
    public void testEnsureRegistryHttpUrlIsTrue(){
        //Given
        String url1 = "http://registor";
        //When
        String result1 = EnvUtil.ensureRegistryHttpUrl(url1);
        //Then
        assertThat("http://registor").isEqualTo(result1);
    }

    @Test
    public void testEnsureRegistryHttpUrlIsNotHttp(){
        //Given
        String url2 = "registerurl";
        //When
        String result2 = EnvUtil.ensureRegistryHttpUrl(url2);
        //Then
        assertThat("https://registerurl").isEqualTo(result2);
    }

    @Test
    public void testStoreTimestamp(
            @Mocked Files files, @Mocked File fileToStoreTimestamp, @Mocked File dir) throws IOException {
        // Given
        new Expectations() {{
            fileToStoreTimestamp.exists() ;
            result = false;
            fileToStoreTimestamp.getParentFile();
            result = dir;
            dir.exists();
            result = true;
        }};
        final Date date = new Date(1445385600000L);
        // When
        storeTimestamp(fileToStoreTimestamp, date);
        // Then
        new Verifications() {{
            files.write(withInstanceOf(Path.class), "1445385600000".getBytes(StandardCharsets.US_ASCII));
        }};
    }

    @Test
    public void testLoadTimestampShouldLoadFromFile() throws Exception {
        // Given
        final File file = new File(EnvUtilTest.class.getResource("/util/loadTimestamp.timestamp").getFile());
        // When
        final Date timestamp = loadTimestamp(file);
        // Then
        assertThat(timestamp).isEqualTo(new Date(1445385600000L));
    }

    @Test
    public  void testIsWindowsFalse(){
        //Given
        new SystemMock().put("os.name", "random");
        //When
        boolean result= EnvUtil.isWindows();
        //Then
        assertThat(result).isFalse();
    }

    @Test
    public  void testIsWindows(){
        //Given
        new SystemMock().put("os.name", "windows");
        //When
        boolean result= EnvUtil.isWindows();
        //Then
        assertThat(result).isTrue();
    }

    @Test
    public void testSystemPropertyRead() {
        System.setProperty("testProperty", "testPropertyValue");
        String propertyValue =
                EnvUtil.getEnvVarOrSystemProperty("testProperty", "defaultValue");
        assertThat( "testPropertyValue").isEqualTo(propertyValue);
        System.clearProperty("testProperty");
    }

    @Test
    public void testDefaultSystemPropertyRead() {
        String propertyValue =
                EnvUtil.getEnvVarOrSystemProperty("testProperty", "defaultValue");
        assertThat( "defaultValue").isEqualTo(propertyValue);
    }
}
