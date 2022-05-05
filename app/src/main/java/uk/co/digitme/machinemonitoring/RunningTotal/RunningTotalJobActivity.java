package uk.co.digitme.machinemonitoring.RunningTotal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.text.SimpleDateFormat;
import java.util.Date;

import uk.co.digitme.machinemonitoring.DataEntryActivity;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.JobActivityBase;
import uk.co.digitme.machinemonitoring.R;


/**
 * The activity shown when the server reports this device's job is in progress
 */
public class RunningTotalJobActivity extends JobActivityBase {
    public static final int JOB_UPDATE_REQUEST_CODE = 9003;
    final String TAG = "RunningTotalJobActivity";

    Button mUpdateTotalButton;
    ActivityResultLauncher<Intent> updateTotalResult;
    Runnable flashUpdateBox;
    int currentQuantity;

    long lastUpdateTimestampSeconds;

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

        currentQuantity = getIntent().getIntExtra("currentQuantity", 0);
        String buttonText = getResources().getString(R.string.update_total_btn) + currentQuantity;
        mUpdateTotalButton.setText(buttonText);

        updateTotalResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> finish());


        Handler h = new Handler();
        flashUpdateBox = new Runnable() {
            boolean alternate = true;
            @Override
            public void run() {
                if (alternate) {
                    mUpdateTotalButton.setBackgroundColor(Color.RED);
                } else {
                    mUpdateTotalButton.setBackgroundColor(Color.LTGRAY);
                }
                alternate = !alternate;
                h.postDelayed(this, 1000);
            }
        };
        lastUpdateTimestampSeconds = (long) getIntent().getDoubleExtra("lastUpdate", 0);
        int updateFrequencySeconds = getIntent().getIntExtra("updateFrequency", 3600);
        long currentTimestampSeconds = System.currentTimeMillis() / 1000;

        long secondsSinceUpdate = currentTimestampSeconds - lastUpdateTimestampSeconds;
        long milliSecondsTillUpdateRequest = (updateFrequencySeconds - secondsSinceUpdate) * 1000;
        new CountDownTimer(milliSecondsTillUpdateRequest, 1000) {

            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "countdown: " + millisUntilFinished);
            }

            public void onFinish() {
                flashUpdateBox.run();
                String updateButtonText = "Update required\nCurrent quantity: " + currentQuantity;
                mUpdateTotalButton.setText(updateButtonText);
            }
        }.start();


    }


    protected void updateTotal() {
        Intent updateTotalIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
        updateTotalIntent.putExtra("requestCode", JOB_UPDATE_REQUEST_CODE);
        updateTotalIntent.putExtra("url", "/android-update-quantity");
        updateTotalIntent.putExtra("numericalInput", true);
        SimpleDateFormat formatter = new SimpleDateFormat("kk:mm");
        String timeString = formatter.format(new Date(lastUpdateTimestampSeconds*1000));
        updateTotalIntent.putExtra("instructionText", "Enter the quantity produced since " + timeString);
        // Pass the requested data from the initial intent
        updateTotalIntent.putExtra("requestedData", getIntent().getStringExtra("requestedDataOnEnd"));
        // The text shown on the send button
        updateTotalIntent.putExtra("sendButtonText", "Update Total");

        updateTotalResult.launch(updateTotalIntent);
    }



}
