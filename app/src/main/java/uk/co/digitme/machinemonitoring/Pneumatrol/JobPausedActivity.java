package uk.co.digitme.machinemonitoring.Pneumatrol;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.EndActivityResponseListener;
import uk.co.digitme.machinemonitoring.Helpers.LoggedInActivity;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.R;

/**
 * The activity shown when the server reports this device's job is paused
 */
public class JobPausedActivity extends LoggedInActivity {

    final String TAG = "JobPausedActivity";

    Spinner mDowntimeReasonsSpinner;
    Button mResumeButton;
    EditText mNotes;

    String jobNumber;

    DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        // Set to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Stop the screen timeout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.pneumatrol_activity_job_paused);

        // Change the colour of the background. The colour is sent by the server
        String colour = getIntent().getStringExtra("colour");
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setBackgroundColor(Color.parseColor(colour));

        // Set the action bar to read the job number
        jobNumber = getIntent().getStringExtra("jobNumber");
        Log.v(TAG, "Job number: " + jobNumber);
        getSupportActionBar().setTitle("Job in progress: " + jobNumber);
        // Set the colour to match the background
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(colour)));

        mDowntimeReasonsSpinner = findViewById(R.id.downtime_reasons_spinner);
        mResumeButton = findViewById(R.id.resume_button);
        mNotes = findViewById(R.id.notes);

        mResumeButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                resumeJob();

            }
        });

        // Get the list of downtime reasons (Sent by the server) and populate the spinner
        ArrayList<String> reasons = getIntent().getStringArrayListExtra("activityCodes");
        ArrayAdapter<String> downtimeReasonsAdapter = new ArrayAdapter<String>
                (this, R.layout.spinner_item, reasons);
        downtimeReasonsAdapter.setDropDownViewResource(R.layout.spinner_item);
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
            String url = dbHelper.getServerAddress() + "/pneumatrolresumejob";
            JSONObject jsonBody = new JSONObject();

            jsonBody.put("downtime_reason", mDowntimeReasonsSpinner.getSelectedItem().toString());
            jsonBody.put("notes", mNotes.getText().toString());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonBody,
                    new EndActivityResponseListener(this),
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