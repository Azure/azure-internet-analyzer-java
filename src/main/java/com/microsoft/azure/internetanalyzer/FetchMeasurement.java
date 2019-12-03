/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import static com.microsoft.azure.internetanalyzer.MeasurementTypes.HTTP;
import static com.microsoft.azure.internetanalyzer.MeasurementTypes.HTTPS;

public class FetchMeasurement implements IMeasurement {

    private static final String defaultMeasurementObjPath = "/apc/";
    private static final String defaultLatencyImgName = "trans.gif";
    private String measurementObjPath;
    private String latencyImageName;

    private int measurementType;
    private String experimentId;
    private Set<FetchUrl> fetchUrls;

    public FetchMeasurement(String measurementEndpoint, int measurementType, String experimentId, String objectPath) {
        if (measurementEndpoint.isEmpty() || !MeasurementTypes.isFetchMeasurementType(measurementType)) {
            throw new IllegalArgumentException("measurementEndpoint is empty or measurementType is invalid");
        }

        this.measurementType = measurementType;
        this.fetchUrls = generateFetchURLs(measurementEndpoint);
        this.experimentId = experimentId;

        if (objectPath.isEmpty())
        {
            this.measurementObjPath = defaultMeasurementObjPath;
            this.latencyImageName = defaultLatencyImgName;
        }
        else
        {
            // object path is in the format {objectPath}{latencyImageName}
            int objectPathSplitIndex = objectPath.lastIndexOf("/") + 1;

            String measurementObjPath = objectPath.substring(0, objectPathSplitIndex);
            if (measurementObjPath.isEmpty())
            {
                this.measurementObjPath = "/";
            }
            else
            {
                this.measurementObjPath = measurementObjPath;
            }

            this.latencyImageName = objectPath.substring(objectPathSplitIndex);
        }
    }

    public Set<FetchUrl> getFetchUrls() {
        return fetchUrls;
    }

    @Override
    public void takeAndReportMeasurements(List<IReportItem> report) throws IOException, CertificateEncodingException {
        URLConnection connection = null;
        for (FetchUrl fetchUrlObj : fetchUrls) {
            FetchReportItem reportItemCold = new FetchReportItem();
            long timeElapsedCold = takeMeasurement(fetchUrlObj, connection, ConnectionType.Cold, reportItemCold);
            reportItemCold.addMeasurementProperties(fetchUrlObj.getCurrentFetchEndpoint(), timeElapsedCold, fetchUrlObj.getMeasurementType(), ConnectionType.Cold.toString(), latencyImageName, experimentId);
            report.add(reportItemCold);

            // only take the warm measurement if the cold measurement succeeds; otherwise if the warm measurement succeeds without a previous cold measurement, it is essentially a cold measurement
            if (timeElapsedCold > 0) {
                FetchReportItem reportItemWarm = new FetchReportItem();
                long timeElapsedWarm = takeMeasurement(fetchUrlObj, connection, ConnectionType.Warm, reportItemWarm);
                reportItemWarm.addMeasurementProperties(fetchUrlObj.getCurrentFetchEndpoint(), timeElapsedWarm, fetchUrlObj.getMeasurementType(), ConnectionType.Warm.toString(), latencyImageName, experimentId);
                report.add(reportItemWarm);
            }
        }
    }

    private long takeMeasurement(FetchUrl fetchUrlObj, URLConnection connection, ConnectionType connectionType, FetchReportItem reportItem) throws IOException, CertificateEncodingException {
        long elapsedTime = -1;

        URL fetchUrl = new URL(fetchUrlObj.getNextFetchUrl());
        long start = System.currentTimeMillis();
        connection = fetchUrl.openConnection();

        try {
            InputStream in = new BufferedInputStream(connection.getInputStream());
            reportItem.addConnectionHeaders(connection, fetchUrl);

            if (drainStream(in)) {
                in.close();
                long finish = System.currentTimeMillis();
                elapsedTime = finish - start;
            }
        } catch (Exception e) {
            if (connection instanceof HttpsURLConnection) {
                elapsedTime = elapsedTime * ((HttpsURLConnection) connection).getResponseCode();
            } else if (connection instanceof HttpURLConnection) {
                elapsedTime = elapsedTime * ((HttpURLConnection) connection).getResponseCode();
            }
        } finally {
            if (connectionType == ConnectionType.Warm || elapsedTime < 0) {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) connection).disconnect();
                } else if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).disconnect();
                }
            }
        }

        return elapsedTime;
    }

    private boolean drainStream(InputStream inputStream) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            while (in.readLine() != null) {
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private Set<FetchUrl> generateFetchURLs(String measurementEndpoint) {

        Set<FetchUrl> result = new HashSet<FetchUrl>();

        if ((HTTP & measurementType) == HTTP) {
            result.add(new FetchUrl(HTTP, measurementEndpoint));
        }

        if ((HTTPS & measurementType) == HTTPS) {
            result.add(new FetchUrl(HTTPS, measurementEndpoint));
        }

        return result;
    }

    // Generates an RFC4122 version 4 GUID
    private String generateAlphaNumericGuidStr() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private enum ConnectionType {
        Warm,
        Cold
    }

    public class FetchUrl {
        private String httpsStr = "https";
        private String httpStr = "http";
        private int measurementType;
        private String measurementEndpoint;
        private String currentFetchEndpoint;

        public FetchUrl(int measurementType, String measurementEndpoint) {
            this.measurementEndpoint = measurementEndpoint;
            this.measurementType = measurementType;
            this.currentFetchEndpoint = "";
        }

        public int getMeasurementType() {

            return measurementType;
        }

        public String getCurrentFetchEndpoint() {
            return currentFetchEndpoint;
        }

        public String getNextFetchUrl() {
            StringBuilder urlPath = new StringBuilder();
            if (measurementType == MeasurementTypes.HTTPS) {
                urlPath.append(httpsStr);
            } else if (measurementType == MeasurementTypes.HTTP) {
                urlPath.append(httpStr);
            }

            urlPath.append("://");

            if (measurementEndpoint.startsWith("*.")) {
                String fqdn = measurementEndpoint.substring(2); // get everything after the "*."
                String measurementEndpointUuid = generateAlphaNumericGuidStr();
                this.currentFetchEndpoint = measurementEndpointUuid;
                urlPath.append(measurementEndpointUuid).append(".").append(fqdn);
            } else {
                urlPath.append(measurementEndpoint);
                this.currentFetchEndpoint = measurementEndpoint;
            }

            urlPath.append(measurementObjPath).append(latencyImageName);

            // append a random guid to avoid cache hits
            urlPath.append("?").append(generateAlphaNumericGuidStr());

            return urlPath.toString();
        }
    }

}