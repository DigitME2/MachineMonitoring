package uk.co.digitme.machinemonitoring;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;


/**
 * The activity shown when the server reports this device's job is in progress
 */
public class JobInProgressActivity extends LoggedInActivity {

    private final String TAG = "JobInProgressActivity";

    Button mPauseButton;
    Button mEndJobButton;
    DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DbHelper(getApplicationContext());
        setContentView(R.layout.activity_job_in_progress);
        // Get the job number (sent by the server) to display in the action bar
        String jobNumber = getIntent().getStringExtra("jobNumber");
        Log.v(TAG, "Job number: " + jobNumber);
        getSupportActionBar().setTitle("Job in progress: " + jobNumber);

        mPauseButton = findViewById(R.id.pause_button);
        mEndJobButton = findViewById(R.id.end_button);

        // Clicking the pause button contacts the server and ends this activity
        mPauseButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                pause();
            }
        });

        // Clicking the end job button contacts the server and ends this activity
        mEndJobButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                endJob();

            }
        });

        // Set the background colour according to the colour sent from the server
        String colour = getIntent().getStringExtra("colour");
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setBackgroundColor(Color.parseColor(colour));

    }

    /**
     * Contacts the server, indicating a job pause has been requested, then finishes this activity
     */
    private void pause(){
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + dbHelper.getServerAddress() + "/androidpausejob";

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
     * Contacts the server, indicating a job end has been requested, then finishes this activity
     */
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
}
