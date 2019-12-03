/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertTrue;

public class InternetAnalyzerClientUnitTest {

    private static final String reportUploadUrlScheme = "http://";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(TestUtils.testPort);

    @Test(expected = IOException.class)
    public void CreateMeasurementAgentTestInvalidConfig() throws IOException, JSONException, CertificateEncodingException {
        String localConfigPath = "/testConfiguration/malformedJsonConfig.txt";
        InternetAnalyzerClient.execute("testMonitorId,", "testTag", new String[]{localConfigPath});
    }

    @Test(expected = IllegalArgumentException.class)
    public void CreateMeasurementAgentEmptyResponseConfig() throws IOException, JSONException, CertificateEncodingException {
        String configResponse = "";
        InternetAnalyzerClient.execute("testMonitorId,", "testTag", configResponse, reportUploadUrlScheme);
    }

    @Test(expected = IOException.class)
    public void CreateMeasurementAgentTestEmptyConfig() throws IOException, JSONException, CertificateEncodingException {
        String localConfigPath = "/testConfiguration/emptyJsonConfig.txt";
        InternetAnalyzerClient.execute("testMonitorId,", "testTag", new String[]{localConfigPath});
    }

    @Test
    public void ExecuteWithSimpleConfigurationTest() throws JSONException, IOException, CertificateEncodingException {

        String[] localConfigPaths = {"/testConfiguration/goodSimpleConfig.txt", "/testConfiguration/goodSimpleConfigHttps.txt", "/testConfiguration/goodSimpleConfigHttp.txt", "/testConfiguration/goodSimpleConfigWithExperimentId.txt"};
        for (String localConfigPath : localConfigPaths) {
            String configContents = TestUtils.GetFileContents(localConfigPath);

            stubFor(get(urlEqualTo(localConfigPath))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(configContents)));

            stubFor(get(urlEqualTo("/apc/trans.gif"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("")));

            stubFor(get(urlMatching(TestUtils.reportUploadPattern))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(TestUtils.reportSuccess)));

            String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
            String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                    new String[]{configurationStr});

            assertTrue(!internetAnalyzerConfigurations.equals(""));

            InternetAnalyzerClient.firstSuccessfulHttpGetResult finalUploadUrls = InternetAnalyzerClient.execute("INTERNET-ANALYZER-TEST", "tag-test", internetAnalyzerConfigurations, reportUploadUrlScheme);
            assertTrue(finalUploadUrls.getResult().equals(TestUtils.reportSuccess));
            TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl());
        }
    }

    @Test
    public void ExecuteWithSimpleConfigurationWithExperimentIdTest() throws JSONException, IOException, CertificateEncodingException {

        String[] localConfigPaths = {"/testConfiguration/goodSimpleConfigWithExperimentId.txt"};
        for (String localConfigPath : localConfigPaths) {
            String configContents = TestUtils.GetFileContents(localConfigPath);

            stubFor(get(urlEqualTo(localConfigPath))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(configContents)));

            stubFor(get(urlEqualTo("/apc/trans.gif"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("")));

            stubFor(get(urlMatching(TestUtils.reportUploadPattern))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(TestUtils.reportSuccess)));

            String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
            String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                    new String[]{configurationStr});

            assertTrue(!internetAnalyzerConfigurations.equals(""));

            InternetAnalyzerClient.firstSuccessfulHttpGetResult finalUploadUrls = InternetAnalyzerClient.execute("INTERNET-ANALYZER-TEST", "tag-test", internetAnalyzerConfigurations, reportUploadUrlScheme);
            assertTrue(finalUploadUrls.getResult().equals(TestUtils.reportSuccess));

            Map<String, String> expectedHeaderMap = new HashMap<>();
            expectedHeaderMap.put("Ex", "dummyExpId");
            TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl(), expectedHeaderMap);
            assertTrue(finalUploadUrls.getFirstSuccessfulUrl().contains("dummyExpId"));
        }
    }

    @Test
    public void ExecuteWithComplexConfigurationTest() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/testConfiguration/goodComplexConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

        stubFor(get(urlEqualTo("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        stubFor(get(urlMatching(TestUtils.reportUploadPattern))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(TestUtils.reportSuccess)));


        String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
        String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                new String[]{configurationStr});

        assertTrue(!internetAnalyzerConfigurations.equals(""));

        InternetAnalyzerClient.firstSuccessfulHttpGetResult finalUploadUrls = InternetAnalyzerClient.execute("INTERNET-ANALYZER-TEST", "tag-test", internetAnalyzerConfigurations, reportUploadUrlScheme);
        assertTrue(finalUploadUrls.getResult().equals(TestUtils.reportSuccess));
        TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl());
    }

    @Test
    public void ExecuteWithConfigurationDefinedObjPathTest() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/testConfiguration/goodObjPathConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

        stubFor(get(urlEqualTo("/test/path/hello.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        stubFor(get(urlMatching(TestUtils.reportUploadPattern))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(TestUtils.reportSuccess)));


        String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
        String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                new String[]{configurationStr});

        assertTrue(!internetAnalyzerConfigurations.equals(""));

        InternetAnalyzerClient.firstSuccessfulHttpGetResult finalUploadUrls = InternetAnalyzerClient.execute("INTERNET-ANALYZER-TEST", "tag-test", internetAnalyzerConfigurations, reportUploadUrlScheme);
        assertTrue(finalUploadUrls.getResult().equals(TestUtils.reportSuccess));

        Map<String, String> expectedHeaderMap = new HashMap<>();
        expectedHeaderMap.put("Object", "hello.gif");

        TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl(), expectedHeaderMap);
    }

    public void ExecuteWithConfigurationEmptyObjPathTest() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/testConfiguration/emptyObjPathConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

        stubFor(get(urlEqualTo("/test/path/hello.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        stubFor(get(urlMatching(TestUtils.reportUploadPattern))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(TestUtils.reportSuccess)));


        String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
        String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                new String[]{configurationStr});

        assertTrue(!internetAnalyzerConfigurations.equals(""));

        InternetAnalyzerClient.firstSuccessfulHttpGetResult finalUploadUrls = InternetAnalyzerClient.execute("INTERNET-ANALYZER-TEST", "tag-test", internetAnalyzerConfigurations, reportUploadUrlScheme);
        assertTrue(finalUploadUrls.getResult().equals(TestUtils.reportSuccess));

        Map<String, String> expectedHeaderMap = new HashMap<>();
        expectedHeaderMap.put("Object", "trans.gif");

        TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl(), expectedHeaderMap);
    }

    @Test
    public void ExecuteWithComplexConfigurationSpaceInTag() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/testConfiguration/goodComplexConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

        stubFor(get(urlEqualTo("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        stubFor(get(urlMatching(TestUtils.reportUploadPattern))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(TestUtils.reportSuccess)));

        String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
        String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                new String[]{configurationStr});

        assertTrue(!internetAnalyzerConfigurations.equals(""));

        InternetAnalyzerClient.firstSuccessfulHttpGetResult finalUploadUrls = InternetAnalyzerClient.execute("INTERNET-ANALYZER-TEST", "tag    test", internetAnalyzerConfigurations, reportUploadUrlScheme);
        assertTrue(finalUploadUrls.getResult().equals(TestUtils.reportSuccess));

        TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl());
    }

    @Test
    public void ExecuteSimpleConfigEmptyEndpointListTest() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/testConfiguration/goodSimpleConfigEmptyEndpointList.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

        stubFor(get(urlEqualTo("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        stubFor(get(urlMatching(TestUtils.reportUploadPattern))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(TestUtils.reportSuccess)));


        String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
        String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                new String[]{configurationStr});

        assertTrue(!internetAnalyzerConfigurations.equals(""));

        InternetAnalyzerClient.firstSuccessfulHttpGetResult finalUploadUrls = InternetAnalyzerClient.execute("INTERNET-ANALYZER-TEST", "tagtest", internetAnalyzerConfigurations, reportUploadUrlScheme);
        assertTrue(finalUploadUrls.getResult().equals(TestUtils.reportSuccess));
        TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ExecuteMissingMonitorIdErrTest() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/testConfiguration/goodComplexConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

        String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
        String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                new String[]{configurationStr});

        assertTrue(!internetAnalyzerConfigurations.equals(""));
        InternetAnalyzerClient.execute("", "tag-test", internetAnalyzerConfigurations, reportUploadUrlScheme);
    }

    @Test
    public void GetConfigurationWithNoConfigurationTest() throws JSONException, IOException {

        String localConfigPath = "/TestConfiguration/emptyConfig.txt";
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(""))); // no configuration returned

        String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
        String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                new String[]{configurationStr});

        assertTrue(internetAnalyzerConfigurations.equals(""));
    }
}