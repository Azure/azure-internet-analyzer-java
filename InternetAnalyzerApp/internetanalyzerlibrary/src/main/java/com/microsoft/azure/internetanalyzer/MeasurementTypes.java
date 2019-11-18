package com.microsoft.azure.internetanalyzer;

public class MeasurementTypes {

    public static final int HTTPS = 1 << 0;
    public static final int HTTP = 1 << 1;
    public static final int RTT = 1 << 2;
    public static final int THROUGHPUT_HTTPS = 1 << 3;
    public static final int THROUGHPUT_HTTP = 1 << 4;
    public static final int TRACERT4 = 1 << 5;
    public static final int TRACERT6 = 1 << 6;
    public static final int XHRHTTPS = 1 << 7;
    public static final int XHRHTTP = 1 << 8;
    public static final int JITTER = 1 << 9;
    public static final int PACKET_LOSS = 1 << 10;
    public static final int DNS_LOOKUP = 1 << 11;

    public static boolean isFetchMeasurementType(int measurementType) {
        return (HTTP & measurementType) == HTTP || (HTTPS & measurementType) == HTTPS;
    }

    public static boolean isSupportedMeasurementType(int measurementType) {
        return isFetchMeasurementType(measurementType);
    }
}