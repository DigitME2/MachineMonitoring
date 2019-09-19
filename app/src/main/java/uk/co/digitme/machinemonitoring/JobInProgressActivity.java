package uk.co.digitme.machinemonitoring;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;


/**
 * The activity shown when the server reports this device's job is in progress
 */
public class JobInProgressActivity extends LoggedInActivity {

    final String TAG = "JobInProgressActivity";

    Spinner activityCodeSpinner;
    Button mEndJobButton;

    String jobNumber;

    DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        setContentView(R.layout.activity_job_in_progress);

        // Change the colour of the background. The colour is sent by the server
        String colour = getIntent().getStringExtra("colour");
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setBackgroundColor(Color.parseColor(colour));

        // Set the action bar to read the job number
        jobNumber = getIntent().getStringExtra("jobNumber");
        Log.v(TAG, "Job number: " + jobNumber);
        getSupportActionBar().setTitle("Job in progress: " + jobNumber);

        activityCodeSpinner = findViewById(R.id.activity_code_spinner);
        mEndJobButton = findViewById(R.id.end_job_button);

        mEndJobButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                endJob();

            }
        });

        // Get the list of downtime reasons (Sent by the server) and populate the spinner
        ArrayList<String> codes = getIntent().getStringArrayListExtra("activityCodes");
        ArrayAdapter<String> activityCodeAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, codes);
        activityCodeAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        activityCodeSpinner.setAdapter(activityCodeAdapter);

        //Set the spinner to show the current activity code
        String current_code = getIntent().getStringExtra("currentActivity");
        activityCodeSpinner.setSelection(codes.indexOf(current_code));

        // As soon as the reason is changed, tell the server
        activityCodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Set a count to ignore the first change, which happens during the creation of the activity
            int count=0;
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (count>0) {
                    updateActivity();
                }
                count++;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }



    private void endJob(){
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + dbHelper.getServerAddress() + "/androidendjob";

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    null,
                    new EndActivityResponseListener(this),
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.v("ErrorListener", String.valueOf(error));
                            Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                        }
                    });

            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
            }
        }
    }


    /**
     * Contacts the server to say the status of the machine has changed
     *
     * Updates the background colour if successful
     */
    private void updateActivity(){



        // Contact the server to inform of the update
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + dbHelper.getServerAddress() + "/androidupdate";
            JSONObject jsonBody = new JSONObject();

            jsonBody.put("selected_activity_code", activityCodeSpinner.getSelectedItem().toString());
            // Send the request. Don't listen for the response and ignore any failures, this isn't a critical update.
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Boolean success;
                            try {
                                // Get the state from the server response
                                success = response.getBoolean("success");
                                Log.d(TAG, "Server response: " + response.toString());
                                if (success){
                                    // Change the colour of the background. The colour is sent by the server
                                    String newColour = response.getString("colour");
                                    View rootView = getWindow().getDecorView().getRootView();
                                    rootView.setBackgroundColor(Color.parseColor(newColour));
                                }
                            } catch (Exception e) {
                                Log.v(TAG, "Failed parsing server response: " + response);
                                return;
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.v("ErrorListener", String.valueOf(error));
                            Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });

            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
                finish();
            }
        }
    }
}
