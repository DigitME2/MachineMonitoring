package uk.co.digitme.machinemonitoring;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.LoggedInActivity;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;


/**
 * The base activity for the job in progress screen
 */
public abstract class JobActivityBase extends LoggedInActivity {

    final String TAG = "JobInProgressActivity";
    public static final int JOB_END_DATA_REQUEST_CODE = 9002;

    Spinner activityCodeSpinner;
    Button mEndJobButton;
    ActivityResultLauncher<Intent> endJobResult;

    String jobNumber;

    public DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Change the colour of the background. The colour is sent by the server
        String colour = getIntent().getStringExtra("colour");
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setBackgroundColor(Color.parseColor(colour));

        // Set the action bar to read the job number
        jobNumber = getIntent().getStringExtra("jobNumber");
        Log.v(TAG, "Job number: " + jobNumber);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Job in progress: " + jobNumber);
        // Set the colour of the action bar to match the background
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(colour)));

        activityCodeSpinner = findViewById(R.id.activity_code_spinner);
        mEndJobButton = findViewById(R.id.end_job_button);

        mEndJobButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                endJob();
            }
        });

        endJobResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    finish();
                });


        // Get the list of downtime reasons (Sent by the server) and populate the spinner
        ArrayList<String> codes = getIntent().getStringArrayListExtra("activityCodes");
        ArrayAdapter<String> activityCodeAdapter;

        if (codes != null) {
            activityCodeAdapter = new ArrayAdapter<>
                    (this, R.layout.spinner_item, codes);
            activityCodeAdapter.setDropDownViewResource(R.layout.spinner_item);
            activityCodeSpinner.setAdapter(activityCodeAdapter);
            //Set the spinner to show the current activity code
            String current_code = getIntent().getStringExtra("currentActivity");
            activityCodeSpinner.setSelection(codes.indexOf(current_code));
        }



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
                // Do nothing
            }
        });
    }


    /**
     * Launches a data input activity to add additional data and then ends the activity
     */
    private void endJob(){
        Intent endJobInfoIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
        endJobInfoIntent.putExtra("requestCode", JOB_END_DATA_REQUEST_CODE);
        endJobInfoIntent.putExtra("url", "/android-end-job");
        endJobInfoIntent.putExtra("numericalInput", true);
        // Pass the requested data from the initial intent
        endJobInfoIntent.putExtra("requestedData", getIntent().getStringExtra("requestedDataOnEnd"));
        // The text shown on the send button
        endJobInfoIntent.putExtra("sendButtonText", "End");

        endJobResult.launch(endJobInfoIntent);
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
            String url = dbHelper.getServerAddress() + "/android-update";
            JSONObject jsonBody = new JSONObject();

            jsonBody.put("selected_activity_code", activityCodeSpinner.getSelectedItem().toString());
            // Send the request. Don't listen for the response and ignore any failures, this isn't a critical update.
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        boolean success;
                        try {
                            // Get the state from the server response
                            success = response.getBoolean("success");
                            Log.d(TAG, "Server response: " + response.toString());
                            if (success){
                                // Change the colour of the background. The colour is sent by the server
                                String newColour = response.getString("colour");
                                View rootView = getWindow().getDecorView().getRootView();
                                rootView.setBackgroundColor(Color.parseColor(newColour));
                                // Set the colour of the action bar to match the background
                                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(newColour)));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.v(TAG, "Failed parsing server response: " + response);
                        }
                    },
                    error -> {
                        Log.v("ErrorListener", String.valueOf(error));
                        Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                        finish();
                    });

            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
                finish();
            }
        }
    }
}