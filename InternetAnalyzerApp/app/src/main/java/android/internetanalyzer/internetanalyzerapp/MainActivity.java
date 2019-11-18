/*---------------------------------------------------------------------------------------------

 *  Copyright (c) Microsoft Corporation. All rights reserved.

 *  Licensed under the MIT License. See License.txt in the project root for license information.

 *--------------------------------------------------------------------------------------------*/
package android.internetanalyzer.internetanalyzerapp;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.microsoft.azure.internetanalyzer.InternetAnalyzerClient;

public class MainActivity extends AppCompatActivity {

    private static final String MONITOR_ID = "INTERNET-ANALYZER-ANDROID-TEST";
    private static final String TAG = "SAMPLE-TAG";
    private static ProgressDialog progressDialog;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fpExecutionBtn = findViewById(R.id.fpExecutionBtn);

        fpExecutionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new internetanalyzerClientActivityAsync().execute();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class internetanalyzerClientActivityAsync extends AsyncTask<String, Void, String> {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... params) {
            progressDialog.setMessage("Executing internetanalyzer...");
            progressDialog.show();

            try {
                InternetAnalyzerClient.execute(MONITOR_ID, TAG, new String[]{"https://aka.ms/InternetAnalyzerDemoConfig"});
                progressDialog.setMessage("Uploaded!");
                progressDialog.show();
            } catch (Exception ex) {
                System.out.println(ex);
                progressDialog.setMessage("Error executing internetanalyzer client: " + ex.toString());
                progressDialog.show();
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.cancel();
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Executing Internet Analyzer client...");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
