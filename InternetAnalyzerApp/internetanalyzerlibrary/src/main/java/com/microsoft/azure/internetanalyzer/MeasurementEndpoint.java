package com.microsoft.azure.internetanalyzer;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.List;

public class MeasurementEndpoint {
    private int weight;
    private String endpoint;
    private int measurementType;
    private String experimentId;

    public MeasurementEndpoint(int weight, String endpoint, int measurementType, String experimentId) {
        this.weight = weight;
        this.endpoint = endpoint;
        this.measurementType = measurementType;
        this.experimentId = experimentId;
    }

    public int getWeight() {
        return weight;
    }

    public void takeAndReportMeasurements(List<IReportItem> reportItems) throws IOException, CertificateEncodingException {
        if (MeasurementTypes.isFetchMeasurementType(measurementType)) {
            FetchMeasurement fetchMeasurement = new FetchMeasurement(endpoint, measurementType, experimentId);
            fetchMeasurement.takeAndReportMeasurements(reportItems);
        }
    }
}
