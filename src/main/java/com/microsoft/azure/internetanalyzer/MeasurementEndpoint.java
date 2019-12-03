package com.microsoft.azure.internetanalyzer;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.List;

public class MeasurementEndpoint {
    private int weight;
    private String endpoint;
    private int measurementType;
    private String experimentId;
    private String objectPath;

    public MeasurementEndpoint(int weight, String endpoint, int measurementType, String experimentId, String objectPath) {
        this.weight = weight;
        this.endpoint = endpoint;
        this.measurementType = measurementType;
        this.experimentId = experimentId;
        this.objectPath = objectPath;
    }

    public int getWeight() {
        return weight;
    }

    public void takeAndReportMeasurements(List<IReportItem> reportItems) throws IOException, CertificateEncodingException {
        if (MeasurementTypes.isFetchMeasurementType(measurementType)) {
            FetchMeasurement fetchMeasurement = new FetchMeasurement(endpoint, measurementType, experimentId, objectPath);
            fetchMeasurement.takeAndReportMeasurements(reportItems);
        }
    }
}
