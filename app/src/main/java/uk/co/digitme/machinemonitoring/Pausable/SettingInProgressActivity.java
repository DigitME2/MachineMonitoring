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

import org.json.JSONObject;

import java.util.Objects;

import uk.co.digitme.machinemonitoring.DataEntryActivity;
import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.EndActivityResponseListener;
import uk.co.digitme.machinemonitoring.Helpers.LoggedInActivity;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.R;

import static uk.co.digitme.machinemonitoring.Default.JobInProgressActivity.JOB_END_DATA_REQUEST_CODE;

public class SettingInProgressActivity extends LoggedInActivity {

    public static final int SETTING_END_DATA_REQUEST_CODE = 9003;
    String jobNumber;

    Button mEndButton;

    DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Stop the screen timeout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.pausable_activity_setting_in_progress);

        dbHelper = new DbHelper(getApplicationContext());

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

        mEndButton = findViewById(R.id.end_job_button);

        mEndButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                endSetting();
            }
        });
    }

    private void endSetting(){
        Intent endJobInfoIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
        endJobInfoIntent.putExtra("requestCode", JOB_END_DATA_REQUEST_CODE);
        endJobInfoIntent.putExtra("url", "/pausableendsetting");
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
