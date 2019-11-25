/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;


public class FetchReportItem implements IReportItem {
    private String requestId = "";
    private long result = -1;
    private int measurementType = -1;
    private String connectionType = "";
    private String measurementObject = "";
    private String experimentId = "";
    private Map<String, String> headerMap = new HashMap<>();

    public void addMeasurementProperties(String requestId, long result, int measurementType, String connectionType, String measurementObject, String experimentId) {
        this.requestId = requestId;
        this.result = result;
        this.measurementType = measurementType;
        this.connectionType = connectionType;
        this.measurementObject = measurementObject;
        this.experimentId = experimentId;
    }

    public void addConnectionHeaders(URLConnection connection, URL url) throws SSLPeerUnverifiedException, CertificateEncodingException, UnknownHostException, NoSuchAlgorithmException {
        if (connection instanceof HttpsURLConnection) {
            Certificate[] certificates = ((HttpsURLConnection) connection).getServerCertificates();
            if (certificates != null && certificates.length > 0) {
                Certificate cert = certificates[0];
                headerMap.put("Ctp", bytesToHex(MessageDigest.getInstance("SHA-1").digest(cert.getEncoded())));
                headerMap.put("Cib", ((X509Certificate) cert).getIssuerX500Principal().getName());
            }
        }

        headerMap.put("Rip", connection.getHeaderField("X-UserHostAddress"));
        headerMap.put("Ep", connection.getHeaderField("X-EndPoint"));
        headerMap.put("Fe", connection.getHeaderField("X-FrontEnd"));
        headerMap.put("Mn", connection.getHeaderField("X-MachineName"));
        headerMap.put("Sip", connection.getHeaderField("X-ServerIP"));
    }

    public JSONObject getFormattedReportItem() throws JSONException {
        JSONObject resultJSONObj = new JSONObject();
        if (!requestId.isEmpty()) {
            resultJSONObj.put("RequestID", requestId);
        }

        if (!connectionType.isEmpty()) {
            resultJSONObj.put("Conn", connectionType);
        }

        if (!measurementObject.isEmpty()) {
            resultJSONObj.put("Object", measurementObject);
        }

        if (!experimentId.isEmpty()) {
            resultJSONObj.put("Ex", experimentId);
        }

        resultJSONObj.put("Result", result);
        resultJSONObj.put("T", measurementType);

        for (String key : headerMap.keySet()) {
            String headerVal = headerMap.get(key);
            if (headerVal != null && headerVal.trim().length() > 0) {
                resultJSONObj.put(key, headerVal.trim());
            }
        }

        return resultJSONObj;
    }

    private String bytesToHex(byte[] hashInBytes) {

        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();

    }
}