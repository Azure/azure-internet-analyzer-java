/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MeasurementAgentUnitTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(TestUtils.testPort);

    @Test
    public void CreateMeasurementAgentTestSimple() throws IOException, JSONException {
        JSONArray expectedUploadEndpoints = new JSONArray(Arrays.asList("localhost:8090/report/r.gif", "localhost:8090/apc/trans.gif"));
        int expectedUploadEndpointCount = 2;
        int expectedMeasurementEndpointCount = 1;
        String localConfigPath = "/testConfiguration/goodSimpleConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);

        MeasurementAgent measurementAgent = new MeasurementAgent(new JSONObject(configContents));
        JSONArray uploadEndpoints = measurementAgent.getUploadEndpoints();

        assertEquals(uploadEndpoints.length(), expectedUploadEndpointCount);
        for (int i = 0; i < expectedUploadEndpointCount; i++) {
            assertEquals(uploadEndpoints.get(i), expectedUploadEndpoints.get(i));
        }

        List<MeasurementEndpoint> measurementEndpoints = measurementAgent.getMeasurementEndpoints();
        assertEquals(measurementEndpoints.size(), expectedMeasurementEndpointCount);
    }

    @Test
    public void CreateMeasurementAgentTestSimpleNotImplementedMeasurementType() throws IOException, JSONException {
        JSONArray expectedUploadEndpoints = new JSONArray(Arrays.asList("localhost:8090/report/r.gif", "localhost:8090/apc/trans.gif"));
        int expectedUploadEndpointCount = 2;
        int expectedMeasurementEndpointCount = 0;
        String localConfigPath = "/testConfiguration/goodSimpleConfigNotImplementedMeasurementType.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);

        MeasurementAgent measurementAgent = new MeasurementAgent(new JSONObject(configContents));
        JSONArray uploadEndpoints = measurementAgent.getUploadEndpoints();

        assertEquals(uploadEndpoints.length(), expectedUploadEndpointCount);
        for (int i = 0; i < expectedUploadEndpointCount; i++) {
            assertEquals(uploadEndpoints.get(i), expectedUploadEndpoints.get(i));
        }

        List<MeasurementEndpoint> measurementEndpoints = measurementAgent.getMeasurementEndpoints();
        assertEquals(measurementEndpoints.size(), expectedMeasurementEndpointCount);
    }

    @Test
    public void CreateMeasurementAgentComplexTest() throws IOException, JSONException {
        JSONArray expectedUploadEndpoints = new JSONArray(Arrays.asList("localhost:8090/report/r.gif"));
        int expectedUploadEndpointCount = 1;
        int expectedMeasurementEndpointCount = 4;
        String localConfigPath = "/testConfiguration/goodComplexConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);

        MeasurementAgent measurementAgent = new MeasurementAgent(new JSONObject(configContents));
        JSONArray uploadEndpoints = measurementAgent.getUploadEndpoints();

        assertEquals(uploadEndpoints.length(), expectedUploadEndpointCount);
        for (int i = 0; i < expectedUploadEndpointCount; i++) {
            assertEquals(uploadEndpoints.get(i), expectedUploadEndpoints.get(i));
        }

        List<MeasurementEndpoint> measurementEndpoints = measurementAgent.getMeasurementEndpoints();
        assertEquals(measurementEndpoints.size(), expectedMeasurementEndpointCount);
    }

    @Test(expected = JSONException.class)
    public void CreateMeasurementAgentEmptyTest() throws IOException, JSONException {

        int expectedUploadEndpointCount = 0;
        int expectedMeasurementEndpointCount = 0;
        String localConfigPath = "/testConfiguration/emptyJsonConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);

        MeasurementAgent measurementAgent = new MeasurementAgent(new JSONObject(configContents));
        JSONArray uploadEndpoints = measurementAgent.getUploadEndpoints();

        assertEquals(uploadEndpoints.length(), expectedUploadEndpointCount);

        List<MeasurementEndpoint> measurementEndpoints = measurementAgent.getMeasurementEndpoints();
        assertEquals(measurementEndpoints.size(), expectedMeasurementEndpointCount);
    }

    @Test(expected = JSONException.class)
    public void CreateMeasurementAgentMissingUploadEndpoints() throws IOException, JSONException {

        int expectedUploadEndpointCount = 0;
        int expectedMeasurementEndpointCount = 0;
        String localConfigPath = "/testConfiguration/missingUploadEndpoints.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);

        MeasurementAgent measurementAgent = new MeasurementAgent(new JSONObject(configContents));
        JSONArray uploadEndpoints = measurementAgent.getUploadEndpoints();

        assertEquals(uploadEndpoints.length(), expectedUploadEndpointCount);

        List<MeasurementEndpoint> measurementEndpoints = measurementAgent.getMeasurementEndpoints();
        assertEquals(measurementEndpoints.size(), expectedMeasurementEndpointCount);
    }

    @Test(expected = JSONException.class)
    public void CreateMeasurementAgentMissingWeightConfig() throws IOException, JSONException {

        int expectedUploadEndpointCount = 2;
        int expectedMeasurementEndpointCount = 0;
        String localConfigPath = "/testConfiguration/missingWeightConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);

        MeasurementAgent measurementAgent = new MeasurementAgent(new JSONObject(configContents));
        JSONArray uploadEndpoints = measurementAgent.getUploadEndpoints();

        assertEquals(uploadEndpoints.length(), expectedUploadEndpointCount);

        List<MeasurementEndpoint> measurementEndpoints = measurementAgent.getMeasurementEndpoints();
        assertEquals(measurementEndpoints.size(), expectedMeasurementEndpointCount);
    }

    @Test
    public void PerformUploadsComplexConfigurationTest() throws JSONException, IOException, CertificateEncodingException {

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

        String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
        String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                new String[]{configurationStr});

        assertTrue(!internetAnalyzerConfigurations.equals(""));

        MeasurementAgent measurementAgent = new MeasurementAgent(new JSONObject(configContents));
        measurementAgent.PerformMeasurements();
        List<IReportItem> reportItems = measurementAgent.getReportItems();
        assertTrue(reportItems.size() > 0);

        for (IReportItem reportItem : reportItems) {
            TestUtils.ValidateReportItem(reportItem.getFormattedReportItem());
        }
    }

    @Test
    public void PerformUploadsSimpleWildcardTest() throws JSONException, IOException, CertificateEncodingException {
        String localConfigPath = "/testConfiguration/goodSimpleWildcardConfig.txt";
        String configContents = TestUtils.GetFileContents(localConfigPath);
        stubFor(get(urlEqualTo(localConfigPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(configContents)));

        stubFor(get(urlMatching("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        String configurationStr = "http://localhost:" + TestUtils.testPort + localConfigPath;
        String internetAnalyzerConfigurations = InternetAnalyzerClient.getConfiguration(
                new String[]{configurationStr});

        assertTrue(!internetAnalyzerConfigurations.equals(""));

        MeasurementAgent measurementAgent = new MeasurementAgent(new JSONObject(configContents));
        measurementAgent.PerformMeasurements();
        List<IReportItem> reportItems = measurementAgent.getReportItems();
        assertTrue(reportItems.size() > 0);

        for (IReportItem reportItem : reportItems) {
            TestUtils.ValidateReportItem(reportItem.getFormattedReportItem());
        }

    }
}
