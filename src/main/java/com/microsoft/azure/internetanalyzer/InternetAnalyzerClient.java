/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class InternetAnalyzerClient {

    private static final String reportUploadUrlScheme = "https://";

    public static firstSuccessfulHttpGetResult execute(String monitorId, String tag, String[] configUrls) throws JSONException, IOException, CertificateEncodingException, IllegalArgumentException {
        return execute(monitorId, tag, getConfiguration(configUrls), reportUploadUrlScheme);
    }

    public static firstSuccessfulHttpGetResult execute(String monitorId, String tag, String configuration, String reportUploadPrefix) throws JSONException, IOException, CertificateEncodingException, IllegalArgumentException {

        //validate monitor Id, configuration, reportUploadPrefix is non-null or empty
        if (monitorId.isEmpty() || configuration.isEmpty() || reportUploadPrefix.isEmpty()) {
            throw new IllegalArgumentException("MonitorId and/or Configuration and/or reportUploadPrefix is empty.");
        }

        MeasurementAgent measurementAgent = new MeasurementAgent(new JSONObject(configuration));
        JSONArray uploadEndpoints = measurementAgent.getUploadEndpoints();
        measurementAgent.PerformMeasurements();
        List<IReportItem> reportItems = measurementAgent.getReportItems();
        return tryUploadReport(formatReport(reportItems, monitorId, tag), uploadEndpoints, reportUploadPrefix);
    }

    public static String getConfiguration(String[] configUrls) throws IOException {
        firstSuccessfulHttpGetResult measurementConfigs = executeFirstSuccessfulHttpGet(configUrls);
        return measurementConfigs.getResult();
    }

    /*
     * Executes the http GET for URLs in chronological order until the first url succeeds.
     * Returns the result as a JSONObject, if result is json parsable
     */
    private static firstSuccessfulHttpGetResult executeFirstSuccessfulHttpGet(String[] Urls) throws IOException {

        for (String Url : Urls) {
            URL requestUri = new URL(Url);
            URLConnection connection = requestUri.openConnection();

            try {
                StringBuilder content = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        content.append(line);
                        content.append(System.lineSeparator());
                    }
                }

                return new firstSuccessfulHttpGetResult(Url, content.toString());
            } catch (Exception ex) {
                throw new IOException("Error executing FirstSuccessful http(s) GET request for: " + Url + " ,Ex: " + ex.toString());
            } finally {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).disconnect();
                } else if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).disconnect();
                }
            }
        }

        // needs error handling
        return new firstSuccessfulHttpGetResult("", "");

    }

    private static firstSuccessfulHttpGetResult tryUploadReport(String reportData, JSONArray uploadEndpoints, String uploadEndpointPrefix) throws JSONException, IOException {
        String[] uploadReports = new String[uploadEndpoints.length()];
        for (int i = 0; i < uploadEndpoints.length(); i++) {
            StringBuilder uploadResult = new StringBuilder();
            uploadResult.append(uploadEndpointPrefix);
            uploadResult.append((String) uploadEndpoints.get(i));
            uploadResult.append("?");
            uploadResult.append(reportData);

            uploadReports[i] = uploadResult.toString();
        }

        return executeFirstSuccessfulHttpGet(uploadReports);
    }

    private static String formatReport(List<IReportItem> reportItems, String monitorId, String tag) throws JSONException, UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        result.append("MonitorId=").append(monitorId);
        result.append("&rid=").append(UUID.randomUUID().toString().replace("-", ""));
        result.append("&w3c=").append("true");
        result.append("&prot=").append("https:");
        result.append("&v=").append("InternetAnalyzer-Android-App:" + InternetAnalyzerClient.class.getPackage().getImplementationVersion());
        result.append("&tag=").append(tag);
        result.append("&DATA=");
        JSONArray data = new JSONArray();

        for (IReportItem reportItem : reportItems) {
            data.put(reportItem.getFormattedReportItem());
        }

        result.append(data.toString());
        return URLEncoder.encode(result.toString(), StandardCharsets.UTF_8.toString());
    }

    public static class firstSuccessfulHttpGetResult {

        private String firstSuccessfulUrl;
        private String result;

        public firstSuccessfulHttpGetResult(String firstSuccessfulUrl, String result) {
            this.firstSuccessfulUrl = firstSuccessfulUrl;
            this.result = result;
        }

        public String getFirstSuccessfulUrl() {

            return firstSuccessfulUrl;
        }

        public String getResult() {

            return result;
        }
    }
}