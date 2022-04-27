package uk.co.digitme.machinemonitoring.RunningTotal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import uk.co.digitme.machinemonitoring.DataEntryActivity;
import uk.co.digitme.machinemonitoring.JobActivityBase;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.R;


/**
 * The activity shown when the server reports this device's job is in progress
 */
public class RunningTotalJobActivity extends JobActivityBase {
    private static final int JOB_UPDATE_REQUEST_CODE = 9003;
    final String TAG = "RunningTotalJobActivity";

    Button mUpdateTotalButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.running_total_activity_job_in_progress);
        super.onCreate(savedInstanceState);

        mUpdateTotalButton = findViewById(R.id.update_total_button);
        mUpdateTotalButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                updateTotal();
            }
        });

    }
    
    
    

    protected void updateTotal() {
//        todo finish, need to write server side mostly to request the correct info
        Intent updateTotalIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
        updateTotalIntent.putExtra("requestCode", JOB_UPDATE_REQUEST_CODE);
        updateTotalIntent.putExtra("url", "/android-update-quantity");
        updateTotalIntent.putExtra("numericalInput", true);
        // Pass the requested data from the initial intent
        updateTotalIntent.putExtra("requestedData", getIntent().getStringExtra("requestedDataOnEnd"));
        // The text shown on the send button
        updateTotalIntent.putExtra("sendButtonText", "End");

        startActivityForResult(updateTotalIntent, JOB_UPDATE_REQUEST_CODE);
    }
}
