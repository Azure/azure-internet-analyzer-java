/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestUtils {

    public static final String reportUploadPattern = "^.*\\/report\\/r.gif?.*$";
    public static final String imgPattern = "^.*\\/apc\\/trans.gif.*$";
    public static final String uuidPattern = "[a-fA-F0-9]{32}";
    public static final int testPort = 8090;
    public static final String reportSuccess = "Report Success" + System.lineSeparator();

    public static String GetFileContents(String localConfigPath) throws IOException {
        String fullFilePath = System.getProperty("user.dir") + localConfigPath;
        File file = new File(fullFilePath);
        assertTrue(file.exists());

        return new String(Files.readAllBytes(Paths.get(fullFilePath)));
    }

    public static void ValidateReportItem(JSONObject reportItem) throws JSONException {
        int measurementType = reportItem.getInt("T");
        assertNotNull(measurementType);
        if (MeasurementTypes.isFetchMeasurementType(measurementType)) {
            ValidateFetchReportItem(reportItem, null);
        }
    }

    public static void ValidateRawFetchReportUrl(String rawUrl) throws JSONException {
        ValidateRawFetchReportUrl(rawUrl, null);
    }

    public static void ValidateRawFetchReportUrl(String rawUrl, Map<String, String> expectedHeaderMap) throws JSONException {
        String decodedUrl = URLDecoder.decode(rawUrl);
        assertTrue(decodedUrl.startsWith("http://localhost:8090/report/r.gif?MonitorId=INTERNET-ANALYZER-TEST"));
        String dataStr = decodedUrl.split("&DATA=")[1];
        JSONArray dataObj = new JSONArray(dataStr);
        for (int i = 0; i < dataObj.length(); i++) {
            JSONObject resultElement = dataObj.getJSONObject(i);
            ValidateFetchReportItem(resultElement, expectedHeaderMap);
        }
    }

    public static void ValidateFetchReportItem(JSONObject reportItem, Map<String, String> expectedCustomValues) throws JSONException {

        String connectionType = reportItem.getString("Conn");
        assertNotNull(connectionType);
        assert (connectionType.equalsIgnoreCase("Cold") || connectionType.equalsIgnoreCase("Warm"));

        String requestId = reportItem.getString("RequestID");
        assertNotNull(requestId);

        String object = reportItem.getString("Object");
        assertNotNull(object);

        int result = reportItem.getInt("Result");
        assertNotNull(result);

        int measurementType = reportItem.getInt("T");
        assertNotNull(measurementType);
        assert (MeasurementTypes.isFetchMeasurementType(measurementType));

        if (expectedCustomValues != null) {
            for (String key : expectedCustomValues.keySet()) {

                try {
                    if (key == "Result") {
                        assertEquals(reportItem.getInt(key), Integer.parseInt(expectedCustomValues.get(key)));
                    } else {
                        assertEquals(reportItem.getString(key), expectedCustomValues.get(key));
                    }
                } catch (JSONException ex) {

                    // custom value is null, so expected custom Value for key should also be null
                    assertNull(expectedCustomValues.get(key));
                }
            }
        }
    }
}