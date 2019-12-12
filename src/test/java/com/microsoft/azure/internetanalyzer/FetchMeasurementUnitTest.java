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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FetchMeasurementUnitTest {
    public static int measurementTypeHttps = 1;
    public static int measurementTypeHttp = 2;
    public static String experimentId = "ex1";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(TestUtils.testPort);

    @Test(expected = IllegalArgumentException.class)
    public void generateFetchUrlsTestEmptyMeasurementEndpoint() {
        new FetchMeasurement("", 3, experimentId, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateFetchUrlsTestInvalidMeasurementType() {
        new FetchMeasurement("testEndpoint", 4, experimentId, "");
    }

    @Test
    public void generateFetchUrlsTestMeasurementType1() {
        FetchMeasurement fetchMeasurement = new FetchMeasurement("testEndpoint", 1, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 1;
        String[] expectedFetchUrls = new String[]{"https://testEndpoint/apc/trans.gif"};
        int[] expectedMeasurementTypes = new int[]{measurementTypeHttps};
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        int i = 0;
        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            int measurementType = fetchUrl.getMeasurementType();
            assertTrue(urlStr.contains(expectedFetchUrls[i]));
            assertEquals(measurementType, expectedMeasurementTypes[i]);
            i++;
        }
    }

    @Test
    public void generateFetchUrlsTestMeasurementType1RandomGuid() {
        FetchMeasurement fetchMeasurement = new FetchMeasurement("*.testEndpoint", 1, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();

        int expectedFetchUrlNum = 1;
        String[] expectedFetchUrlPrefix = new String[]{"https://"};
        String[] expectedFetchUrlSuffix = new String[]{"/apc/trans.gif"};
        int[] expectedMeasurementTypes = new int[]{measurementTypeHttps};
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        int i = 0;
        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            int measurementType = fetchUrl.getMeasurementType();
            assertTrue(urlStr.startsWith(expectedFetchUrlPrefix[i]));
            assertTrue(urlStr.contains(expectedFetchUrlSuffix[i]));
            assertTrue(urlStr.length() > expectedFetchUrlPrefix[i].length() + expectedFetchUrlSuffix[i].length());
            assertEquals(measurementType, expectedMeasurementTypes[i]);
            i++;
        }
    }

    @Test
    public void generateFetchUrlsTestMeasurementType2() {
        FetchMeasurement fetchMeasurement = new FetchMeasurement("testEndpoint2", 2, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 1;
        String[] expectedFetchUrls = new String[]{"http://testEndpoint2/apc/trans.gif"};
        int[] expectedMeasurementTypes = new int[]{measurementTypeHttp};
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        int i = 0;
        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            int measurementType = fetchUrl.getMeasurementType();
            assertTrue(urlStr.contains(expectedFetchUrls[i]));
            assertEquals(measurementType, expectedMeasurementTypes[i]);
            i++;
        }
    }

    @Test
    public void generateFetchUrlsTestMeasurementType3() {
        FetchMeasurement fetchMeasurement = new FetchMeasurement("test.Endpoint", 3, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 2;
        List<String> expectedUrls = new ArrayList<String>();
        expectedUrls.add("http://test.Endpoint/apc/trans.gif");
        expectedUrls.add("https://test.Endpoint/apc/trans.gif");
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            assertTrue(urlStr.contains(expectedUrls.get(0)) || urlStr.contains(expectedUrls.get(1)));
        }
    }

    @Test
    public void generateFetchUrlsTestMeasurementType3RandomGuid() {
        FetchMeasurement fetchMeasurement = new FetchMeasurement("*.testEndpoint", 3, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 2;
        String expectedUrlSuffix = ".testEndpoint/apc/trans.gif";
        String expectedUrlPrefixHttp = "http://";
        String expectedUrlPrefixHttps = "https://";

        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            assertTrue(urlStr.contains(expectedUrlSuffix));
            assertTrue(urlStr.startsWith(expectedUrlPrefixHttps) || urlStr.startsWith(expectedUrlPrefixHttp));
            assertTrue(urlStr.length() > expectedUrlSuffix.length() + expectedUrlPrefixHttps.length());
        }
    }

    @Test
    public void generateFetchUrlsTestMeasurementWithMeasurementObjPath() {
        FetchMeasurement fetchMeasurement = new FetchMeasurement("*.testEndpoint", 3, experimentId, "/myPath/testObj.gif");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 2;
        String expectedUrlSuffix = ".testEndpoint/myPath/testObj.gif";
        String expectedUrlPrefixHttp = "http://";
        String expectedUrlPrefixHttps = "https://";

        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            assertTrue(urlStr.contains(expectedUrlSuffix));
            assertTrue(urlStr.startsWith(expectedUrlPrefixHttps) || urlStr.startsWith(expectedUrlPrefixHttp));
            assertTrue(urlStr.length() > expectedUrlSuffix.length() + expectedUrlPrefixHttps.length());
        }
    }

    @Test
    public void generateFetchUrlsTestMeasurementWithNoPathWithObjImg() {
        FetchMeasurement fetchMeasurement = new FetchMeasurement("*.testEndpoint", 3, experimentId, "testObj.gif");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 2;
        String expectedUrlSuffix = ".testEndpoint/testObj.gif";
        String expectedUrlPrefixHttp = "http://";
        String expectedUrlPrefixHttps = "https://";

        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            assertTrue(urlStr.contains(expectedUrlSuffix));
            assertTrue(urlStr.startsWith(expectedUrlPrefixHttps) || urlStr.startsWith(expectedUrlPrefixHttp));
            assertTrue(urlStr.length() > expectedUrlSuffix.length() + expectedUrlPrefixHttps.length());
        }
    }

    @Test
    public void generateFetchUrlsTestMeasurementWithInvalidMeasurementObjPathStillSucceeeds() {
        FetchMeasurement fetchMeasurement = new FetchMeasurement("*.testEndpoint", 3, experimentId, "////////ha!/");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 2;
        String expectedUrlSuffix = ".testEndpoint////////ha!/";
        String expectedUrlPrefixHttp = "http://";
        String expectedUrlPrefixHttps = "https://";

        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            assertTrue(urlStr.contains(expectedUrlSuffix));
            assertTrue(urlStr.startsWith(expectedUrlPrefixHttps) || urlStr.startsWith(expectedUrlPrefixHttp));
            assertTrue(urlStr.length() > expectedUrlSuffix.length() + expectedUrlPrefixHttps.length());
        }
    }

    @Test
    public void takeAndReportMeasurementAllHeadersReturned() throws IOException, JSONException, CertificateEncodingException {

        String testUserAddress = "test-host-user-address";
        String testEndpoint = "test-endpoint";
        String testFrontEnd = "test-frontend";
        String testMachineName = "test-machine-name";
        String testServerIP = "test-server-ip";
        stubFor(get(urlPathMatching("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("x-userhostaddress", testUserAddress)
                        .withHeader("X-EndPoint", testEndpoint)
                        .withHeader("x-frontend", testFrontEnd)
                        .withHeader("X-MachineName", testMachineName)
                        .withHeader("X-ServerIP", testServerIP)
                        .withBody("")));

        Map<String, String> expectedHeaderMap = new HashMap<>();
        expectedHeaderMap.put("Rip", testUserAddress);
        expectedHeaderMap.put("Ep", testEndpoint);
        expectedHeaderMap.put("Fe", testFrontEnd);
        expectedHeaderMap.put("Mn", testMachineName);
        expectedHeaderMap.put("Sip", testServerIP);

        String measurementEndpoint = "localhost:" + TestUtils.testPort + "";
        FetchMeasurement fetchMeasurement = new FetchMeasurement(measurementEndpoint, measurementTypeHttp, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 1;
        String measurementObject = "trans.gif";
        String[] expectedFetchUrlPrefix = new String[]{"http://"};
        String[] expectedFetchUrlSuffix = new String[]{"apc/" + measurementObject};
        int[] expectedMeasurementTypes = new int[]{measurementTypeHttp};
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        int i = 0;
        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            int measurementType = fetchUrl.getMeasurementType();
            assertTrue(urlStr.startsWith(expectedFetchUrlPrefix[i]));
            assertTrue(urlStr.contains(expectedFetchUrlSuffix[i]));
            assertTrue(urlStr.length() > expectedFetchUrlPrefix[i].length() + expectedFetchUrlSuffix[i].length());
            assertEquals(measurementType, expectedMeasurementTypes[i]);
            i++;
        }

        List<IReportItem> reportItems = new ArrayList<IReportItem>();
        fetchMeasurement.takeAndReportMeasurements(reportItems);

        assertTrue(reportItems.size() > 0);
        for (IReportItem reportItem : reportItems) {
            TestUtils.ValidateFetchReportItem(reportItem.getFormattedReportItem(), expectedHeaderMap);
        }
    }

    @Test
    public void takeAndReportMeasurementSomeHeadersReturned() throws IOException, JSONException, CertificateEncodingException {

        String testEndpoint = "test-endpoint";
        String testFrontEnd = "test-frontend";
        stubFor(get(urlPathMatching("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("X-EndPoint", testEndpoint)
                        .withHeader("X-FrontEnd", testFrontEnd)
                        .withHeader("X-MachineName", "")
                        .withHeader("X-ServerIP", "            ")
                        .withBody("")));

        Map<String, String> expectedHeaderMap = new HashMap<>();
        expectedHeaderMap.put("Rip", null);
        expectedHeaderMap.put("Ep", testEndpoint);
        expectedHeaderMap.put("Fe", testFrontEnd);
        expectedHeaderMap.put("Mn", null);
        expectedHeaderMap.put("Sip", null);

        String measurementEndpoint = "localhost:" + TestUtils.testPort + "";
        FetchMeasurement fetchMeasurement = new FetchMeasurement(measurementEndpoint, measurementTypeHttp, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 1;
        String measurementObject = "trans.gif";
        String[] expectedFetchUrlPrefix = new String[]{"http://"};
        String[] expectedFetchUrlSuffix = new String[]{"apc/" + measurementObject};
        int[] expectedMeasurementTypes = new int[]{measurementTypeHttp};
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        int i = 0;

        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            int measurementType = fetchUrl.getMeasurementType();
            assertTrue(urlStr.startsWith(expectedFetchUrlPrefix[i]));
            assertTrue(urlStr.contains(expectedFetchUrlSuffix[i]));
            assertTrue(urlStr.length() > expectedFetchUrlPrefix[i].length() + expectedFetchUrlSuffix[i].length());
            assertEquals(measurementType, expectedMeasurementTypes[i]);
            i++;
        }

        List<IReportItem> reportItems = new ArrayList<IReportItem>();
        fetchMeasurement.takeAndReportMeasurements(reportItems);

        assertTrue(reportItems.size() > 0);
        for (IReportItem reportItem : reportItems) {
            TestUtils.ValidateFetchReportItem(reportItem.getFormattedReportItem(), expectedHeaderMap);
        }
    }


    @Test
    public void takeAndReportMeasurementTestNoHeadersReturned() throws IOException, JSONException, CertificateEncodingException {

        stubFor(get(urlPathMatching("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("")));

        String measurementEndpoint = "localhost:" + TestUtils.testPort + "";
        FetchMeasurement fetchMeasurement = new FetchMeasurement(measurementEndpoint, measurementTypeHttp, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 1;
        String measurementObject = "trans.gif";
        String[] expectedFetchUrlPrefix = new String[]{"http://"};
        String[] expectedFetchUrlSuffix = new String[]{"apc/" + measurementObject};
        int[] expectedMeasurementTypes = new int[]{measurementTypeHttp};
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        int i = 0;
        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            int measurementType = fetchUrl.getMeasurementType();
            assertTrue(urlStr.startsWith(expectedFetchUrlPrefix[i]));
            assertTrue(urlStr.contains(expectedFetchUrlSuffix[i]));
            assertTrue(urlStr.length() > expectedFetchUrlPrefix[i].length() + expectedFetchUrlSuffix[i].length());
            assertEquals(measurementType, expectedMeasurementTypes[i]);
            i++;
        }

        List<IReportItem> reportItems = new ArrayList<IReportItem>();
        fetchMeasurement.takeAndReportMeasurements(reportItems);

        assertTrue(reportItems.size() > 0);
        for (IReportItem reportItem : reportItems) {
            TestUtils.ValidateFetchReportItem(reportItem.getFormattedReportItem(), null);
        }
    }

    @Test
    public void takeAndReportMeasurementInvalidEndpointsNoErr() throws IOException, CertificateEncodingException {

        stubFor(get(urlPathMatching("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(404) // asset not found!
                        .withHeader("Content-Type", "text/xml")
                        .withBody("")));

        String measurementEndpoint = "localhost:" + TestUtils.testPort + "";
        FetchMeasurement fetchMeasurement = new FetchMeasurement(measurementEndpoint, measurementTypeHttp, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 1;
        String measurementObject = "trans.gif";
        String[] expectedFetchUrlPrefix = new String[]{"http://"};
        String[] expectedFetchUrlSuffix = new String[]{"apc/" + measurementObject};
        int[] expectedMeasurementTypes = new int[]{measurementTypeHttp};
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        int i = 0;

        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            String currentMeasurementEndpoint = fetchUrl.getCurrentFetchEndpoint();
            assertTrue(!urlStr.equals(currentMeasurementEndpoint));
            assertTrue(urlStr.indexOf(currentMeasurementEndpoint) != -1); // index of returns -1 if value is not found
            int measurementType = fetchUrl.getMeasurementType();
            assertTrue(urlStr.startsWith(expectedFetchUrlPrefix[i]));
            assertTrue(urlStr.contains(expectedFetchUrlSuffix[i]));
            assertTrue(urlStr.length() > expectedFetchUrlPrefix[i].length() + expectedFetchUrlSuffix[i].length());
            assertEquals(measurementType, expectedMeasurementTypes[i]);
            i++;
        }

        List<IReportItem> reportItems = new ArrayList<IReportItem>();

        int expectedReportItems = 1;

        fetchMeasurement.takeAndReportMeasurements(reportItems);
        assertEquals(reportItems.size(), expectedReportItems);
    }

    @Test
    public void takeAndReportMeasurementServerErr500Result() throws IOException, CertificateEncodingException, JSONException {

        stubFor(get(urlPathMatching("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(500) // server error!
                        .withHeader("Content-Type", "text/xml")
                        .withBody("")));

        String measurementEndpoint = "localhost:" + TestUtils.testPort + "";
        FetchMeasurement fetchMeasurement = new FetchMeasurement(measurementEndpoint, measurementTypeHttp, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 1;
        String measurementObject = "trans.gif";
        String[] expectedFetchUrlPrefix = new String[]{"http://"};
        String[] expectedFetchUrlSuffix = new String[]{"apc/" + measurementObject};
        int[] expectedMeasurementTypes = new int[]{measurementTypeHttp};
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        int i = 0;

        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            String currentMeasurementEndpoint = fetchUrl.getCurrentFetchEndpoint();
            assertTrue(!urlStr.equals(currentMeasurementEndpoint));
            assertTrue(urlStr.indexOf(currentMeasurementEndpoint) != -1); // index of returns -1 if value is not found
            int measurementType = fetchUrl.getMeasurementType();
            assertTrue(urlStr.startsWith(expectedFetchUrlPrefix[i]));
            assertTrue(urlStr.contains(expectedFetchUrlSuffix[i]));
            assertTrue(urlStr.length() > expectedFetchUrlPrefix[i].length() + expectedFetchUrlSuffix[i].length());
            assertEquals(measurementType, expectedMeasurementTypes[i]);
            i++;
        }

        List<IReportItem> reportItems = new ArrayList<IReportItem>();

        int expectedReportItems = 1;

        fetchMeasurement.takeAndReportMeasurements(reportItems);
        assertEquals(reportItems.size(), expectedReportItems);
        assertEquals(reportItems.get(0).getFormattedReportItem().get("Result"), (long) -500);
        assertEquals(reportItems.get(0).getFormattedReportItem().get("Conn"), "cold");
        assertEquals(reportItems.get(0).getFormattedReportItem().get("Ex"), "ex1");
    }

    @Test
    public void takeAndReportMeasurementSimpleWildcard() throws IOException, CertificateEncodingException {
        stubFor(get(urlEqualTo("/apc/trans.gif"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("")));

        String measurementEndpoint = "*.test.com";
        FetchMeasurement fetchMeasurement = new FetchMeasurement(measurementEndpoint, measurementTypeHttp, experimentId, "");
        Set<FetchMeasurement.FetchUrl> fetchUrls = fetchMeasurement.getFetchUrls();
        int expectedFetchUrlNum = 1;
        String measurementObject = "trans.gif";
        String[] expectedFetchUrlPrefix = new String[]{"http://"};
        String[] expectedFetchUrlSuffix = new String[]{"apc/" + measurementObject};
        int[] expectedMeasurementTypes = new int[]{measurementTypeHttp};
        assertEquals(fetchUrls.size(), expectedFetchUrlNum);

        int i = 0;

        // validate fetch urls
        for (FetchMeasurement.FetchUrl fetchUrl : fetchUrls) {
            String urlStr = fetchUrl.getNextFetchUrl();
            String currentMeasurementEndpoint = fetchUrl.getCurrentFetchEndpoint();
            assertTrue(!urlStr.equals(currentMeasurementEndpoint));
            assertTrue(urlStr.indexOf(currentMeasurementEndpoint) != -1); // index of returns -1 if value is not found
            int measurementType = fetchUrl.getMeasurementType();
            currentMeasurementEndpoint.matches(TestUtils.uuidPattern); // tests that (current) measurement type is of type uuid
            assertTrue(urlStr.startsWith(expectedFetchUrlPrefix[i]));
            assertTrue(urlStr.contains(expectedFetchUrlSuffix[i]));
            assertTrue(urlStr.length() > expectedFetchUrlPrefix[i].length() + expectedFetchUrlSuffix[i].length());
            assertEquals(measurementType, expectedMeasurementTypes[i]);
            i++;
        }

        List<IReportItem> reportItems = new ArrayList<IReportItem>();

        int expectedReportItems = 2;
        fetchMeasurement.takeAndReportMeasurements(reportItems);
        assertEquals(reportItems.size(), expectedReportItems);
    }
}