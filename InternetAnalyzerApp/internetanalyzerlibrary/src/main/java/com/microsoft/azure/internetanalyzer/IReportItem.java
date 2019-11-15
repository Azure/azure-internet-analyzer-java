/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package com.microsoft.azure.internetanalyzer;

import org.json.JSONException;
import org.json.JSONObject;

public interface IReportItem {

    /**
     * Gets the formatted report item
     *
     * @return JSON object representation of the report item
     */
    JSONObject getFormattedReportItem() throws JSONException;
}