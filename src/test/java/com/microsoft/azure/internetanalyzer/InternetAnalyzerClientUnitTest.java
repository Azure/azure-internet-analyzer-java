/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.cert.CertificateEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InternetAnalyzerClientUnitTest {

    private static final String reportUploadUrlScheme = "http://";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(TestUtils.testPort);

    @Test(expected = IOException.class)
    public void CreateMeasurementAgentTestInvalidConfig() throws IOException, JSONException, CertificateEncodingException {
        String localConfigPath = "/src/test/fixtures/malformedJsonConfig.txt";
        InternetAnalyzerClient.execute("testMonitorId,", "testTag", new String[]{localConfigPath});
    }

    @Test(expected = IllegalArgumentException.class)
    public void CreateMeasurementAgentEmptyResponseConfig() throws IOException, JSONException, CertificateEncodingException {
        String configResponse = "";
        InternetAnalyzerClient.execute("testMonitorId,", "testTag", configResponse, reportUploadUrlScheme);
    }

    @Test(expected = IOException.class)
    public void CreateMeasurementAgentTestEmptyConfig() throws IOException, JSONException, CertificateEncodingException {
        String localConfigPath = "/src/test/fixtures/emptyJsonConfig.txt";
        InternetAnalyzerClient.execute("testMonitorId,", "testTag", new String[]{localConfigPath});
    }

    @Test
    public void ExecuteWithSimpleConfigurationTest() throws JSONException, IOException, CertificateEncodingException {

        String[] localConfigPaths = {"/src/test/fixtures/goodSimpleConfig.txt", "/src/test/fixtures/goodSimpleConfigHttps.txt", "/src/test/fixtures/goodSimpleConfigHttp.txt", "/src/test/fixtures/goodSimpleConfigWithExperimentId.txt"};
        for (String localConfigPath : localConfigPaths) {
            String configContents = TestUtils.GetFileContents(localConfigPath);

            stubFor(get(urlEqualTo(localConfigPath))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(configContents)));

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

        String[] localConfigPaths = {"/src/test/fixtures/goodSimpleConfigWithExperimentId.txt"};
        for (String localConfigPath : localConfigPaths) {
            String configContents = TestUtils.GetFileContents(localConfigPath);

            stubFor(get(urlEqualTo(localConfigPath))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody(configContents)));

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

            Map<String, String> expectedCustomValues = new HashMap<>();
            expectedCustomValues.put("Ex", "dummyExpId");
            TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl(), expectedCustomValues);
            assertTrue(finalUploadUrls.getFirstSuccessfulUrl().contains("dummyExpId"));
        }
    }

    @Test
    public void ExecuteWithComplexConfigurationTest() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/src/test/fixtures/goodComplexConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

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

        String localConfigPath = "/src/test/fixtures/goodObjPathConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

        stubFor(get(urlMatching("^.*\\/test\\/path\\/hello.gif?.*$"))
                .willReturn(aResponse()
                        .withStatus(200)));

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

        Map<String, String> expectedCustomValues = new HashMap<>();
        expectedCustomValues.put("Object", "hello.gif");

        TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl(), expectedCustomValues);

        String decodedUrl = URLDecoder.decode(finalUploadUrls.getFirstSuccessfulUrl());
        String dataStr = decodedUrl.split("&DATA=")[1];

        // For test debugging
        System.out.println(dataStr);

        JSONArray dataObj = new JSONArray(dataStr);

        for (int i = 0; i < dataObj.length(); i++) {
            JSONObject resultElement = dataObj.getJSONObject(i);
            int result = resultElement.getInt("Result");
            assertTrue(result >= 0);
        }
    }

    @Test
    public void ExecuteWithConfigurationEmptyObjPathTest() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/src/test/fixtures/emptyObjPathConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

        stubFor(get(urlMatching("^.*\\/apc\\/trans.gif?.*$"))
                .willReturn(aResponse()
                        .withStatus(200)));

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

        Map<String, String> expectedCustomValues = new HashMap<>();
        expectedCustomValues.put("Object", "trans.gif");

        TestUtils.ValidateRawFetchReportUrl(finalUploadUrls.getFirstSuccessfulUrl(), expectedCustomValues);

        String decodedUrl = URLDecoder.decode(finalUploadUrls.getFirstSuccessfulUrl());
        String dataStr = decodedUrl.split("&DATA=")[1];

        // For test debugging
        System.out.println(dataStr);

        JSONArray dataObj = new JSONArray(dataStr);

        for (int i = 0; i < dataObj.length(); i++) {
            JSONObject resultElement = dataObj.getJSONObject(i);
            int result = resultElement.getInt("Result");
            assertTrue(result >= 0);
        }
    }

    @Test
    public void ExecuteWithComplexConfigurationSpaceInTag() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/src/test/fixtures/goodComplexConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

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

        String localConfigPath = "/src/test/fixtures/goodSimpleConfigEmptyEndpointList.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

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
        String uploadUrl = finalUploadUrls.getFirstSuccessfulUrl();
        TestUtils.ValidateRawFetchReportUrl(uploadUrl);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ExecuteMissingMonitorIdErrTest() throws JSONException, IOException, CertificateEncodingException {

        String localConfigPath = "/src/test/fixtures/goodComplexConfig.txt";
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

        String localConfigPath = "/src/test/fixtures/emptyConfig.txt";
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