/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.util.List;

public interface IMeasurement {

    /**
     * Takes and reports measurements
     *
     * @param report - list of report items
     * @return Nothing
     * @throws IOException On input error
     * @see IOException
     */
    void takeAndReportMeasurements(List<IReportItem> report) throws IOException, CertificateEncodingException;
}