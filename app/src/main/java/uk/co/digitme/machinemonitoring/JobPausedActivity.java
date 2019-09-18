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
 * The activity shown when the server reports this device's job is paused
 */
public class JobPausedActivity extends LoggedInActivity {

    final String TAG = "JobPausedActivity";

    Spinner mDowntimeReasonsSpinner;
    Button mResumeButton;

    String jobNumber;

    DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        setContentView(R.layout.activity_job_paused);

        // Set the action bar to read the job number
        jobNumber = getIntent().getStringExtra("jobNumber");
        Log.v(TAG, "Job number: " + jobNumber);
        getSupportActionBar().setTitle("Job in progress: " + jobNumber);

        mDowntimeReasonsSpinner = findViewById(R.id.downtime_reasons_spinner);
        mResumeButton = findViewById(R.id.resume_button);

        mResumeButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                resumeJob();

            }
        });

        // Get the list of downtime reasons (Sent by the server) and populate the spinner
        ArrayList<String> reasons = getIntent().getStringArrayListExtra("downtimeReasons");
        ArrayAdapter<String> downtimeReasonsAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, reasons);
        downtimeReasonsAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        mDowntimeReasonsSpinner.setAdapter(downtimeReasonsAdapter);

        // As soon as the reason is changed, tell the server
        mDowntimeReasonsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateReason();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


    }

    /**
     * Contacts the server indicating the job has been resumed, and ends this activity
     */
    private void resumeJob(){
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + dbHelper.getServerAddress() + "/androidresumejob";
            JSONObject jsonBody = new JSONObject();

            jsonBody.put("downtime_reason", mDowntimeReasonsSpinner.getSelectedItem().toString());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonBody,
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
     * Contacts the server to say the reason for this pause has been updated
     * Also changes the background colour to match
     */
    private void updateReason(){
        try {
            ArrayList<String> colours = getIntent().getStringArrayListExtra("colours");
            int spinnerId = (int) mDowntimeReasonsSpinner.getSelectedItemId();
            String colour = colours.get(spinnerId);
            // Set the background colour
            View rootView = getWindow().getDecorView().getRootView();
            rootView.setBackgroundColor(Color.parseColor(colour));
        } catch (Exception e){
            Log.e(TAG, e.toString());
        }
    }

}
