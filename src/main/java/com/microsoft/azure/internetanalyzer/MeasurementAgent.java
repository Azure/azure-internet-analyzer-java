/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MeasurementAgent {
    private List<MeasurementEndpoint> measurementEndpoints = new ArrayList<MeasurementEndpoint>();
    private List<IReportItem> reportItems = new ArrayList<IReportItem>();
    private int sumEndpointWeight = 0;
    private Random rand = new Random();
    private int measurementNum;
    private JSONArray uploadEndpoints;

    public MeasurementAgent(JSONObject measurementConfigurations) throws JSONException {
        this.measurementNum = measurementConfigurations.getInt("n");
        this.uploadEndpoints = measurementConfigurations.getJSONArray("r");
        JSONArray measurementEndpointsArr = measurementConfigurations.getJSONArray("e");
        for (int i = 0; i < measurementEndpointsArr.length(); i++) {
            JSONObject measurementEndpointObj = measurementEndpointsArr.getJSONObject(i);
            int measurementEndpointType = measurementEndpointObj.getInt("m");
            int measurementEndpointWeight = measurementEndpointObj.getInt("w");
            String experimentId = measurementEndpointObj.optString("ex");
            String objectPath = measurementEndpointObj.optString("o");

            if (MeasurementTypes.isSupportedMeasurementType(measurementEndpointType)) {
                this.measurementEndpoints.add(new MeasurementEndpoint(
                        measurementEndpointWeight,
                        measurementEndpointObj.getString("e"),
                        measurementEndpointType,
                        experimentId,
                        objectPath));
                sumEndpointWeight += measurementEndpointWeight;
            }
        }

        if (measurementEndpointsArr.length() < this.measurementNum) {
            this.measurementNum = measurementEndpointsArr.length();
        }
    }

    public JSONArray getUploadEndpoints() {
        return uploadEndpoints;
    }

    public List<MeasurementEndpoint> getMeasurementEndpoints() {
        return measurementEndpoints;
    }

    public void PerformMeasurements() throws IOException, CertificateEncodingException {
        if (measurementNum <= measurementEndpoints.size()) {
            for (int i = 0; i < measurementNum; i++) {
                MeasurementEndpoint measurementEndpoint = getRandomMeasurementEndpoint();
                measurementEndpoint.takeAndReportMeasurements(reportItems);
            }
        }
    }

    public List<IReportItem> getReportItems() {
        return reportItems;
    }

    private MeasurementEndpoint getRandomMeasurementEndpoint() {
        if (!measurementEndpoints.isEmpty()) {
            int randomNum = rand.nextInt(sumEndpointWeight);
            int accumulatedWeight = 0;
            for (MeasurementEndpoint endpoint : measurementEndpoints) {
                int endpointWeight = endpoint.getWeight();
                accumulatedWeight += endpointWeight;
                if (randomNum < accumulatedWeight) {
                    sumEndpointWeight = sumEndpointWeight - endpointWeight;
                    measurementEndpoints.remove(endpoint);
                    return endpoint;
                }
            }
        }

        // should never reach this point
        throw new IllegalStateException("getRandomMeasurementEndpoint() cannot return valid measurement endpoint.");
    }
}
