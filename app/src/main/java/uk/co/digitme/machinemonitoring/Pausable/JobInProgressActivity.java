package uk.co.digitme.machinemonitoring.Pausable;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import java.util.Objects;

import uk.co.digitme.machinemonitoring.DataEntryActivity;
import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.EndActivityResponseListener;
import uk.co.digitme.machinemonitoring.Helpers.LoggedInActivity;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.R;


/**
 * The activity shown when the server reports this device's job is in progress
 */
public class JobInProgressActivity extends LoggedInActivity {

    final String TAG = "JobInProgressActivity";
    public static final int JOB_END_DATA_REQUEST_CODE = 9002;

    Button mPauseJobButton;
    Button mEndJobButton;

    String jobNumber;
    String currentActivity;

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
        setContentView(R.layout.pausable_activity_job_in_progress);

        // Change the colour of the background. The colour is sent by the server
        String colour = getIntent().getStringExtra("colour");
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setBackgroundColor(Color.parseColor(colour));

        // Set the action bar to read the job number
        jobNumber = getIntent().getStringExtra("jobNumber");
        currentActivity = getIntent().getStringExtra("currentActivity");
        Log.v(TAG, "Job number: " + jobNumber);
        Objects.requireNonNull(getSupportActionBar()).setTitle(jobNumber + " : " + currentActivity);
        // Set the colour of the action bar to match the background
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(colour)));


        mEndJobButton = findViewById(R.id.end_job_button);
        mPauseJobButton = findViewById(R.id.pause_job_button);

        mEndJobButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                endJob();
            }
        });

        mPauseJobButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                pauseJob();
            }
        });


    }

    /**
     * Contacts the server, indicating a job pause has been requested, then finishes this activity
     */
    private void pauseJob(){
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = dbHelper.getServerAddress() + "/pausable-pause-job";

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


    private void endJob() {
        Intent endJobInfoIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
        endJobInfoIntent.putExtra("requestCode", JOB_END_DATA_REQUEST_CODE);
        endJobInfoIntent.putExtra("url", "/android-end-job");
        endJobInfoIntent.putExtra("numericalInput", true);
        // Pass the requested data from the initial intent
        endJobInfoIntent.putExtra("requestedData", getIntent().getStringExtra("requestedDataOnEnd"));
        // The text shown on the send button
        endJobInfoIntent.putExtra("sendButtonText", "End");

        startActivityForResult(endJobInfoIntent, JOB_END_DATA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // When the job end information returns, send the info to the server to end the job
        if (requestCode == JOB_END_DATA_REQUEST_CODE) {
            if (resultCode == RESULT_OK){
                finish();
            }
        }
    }
}