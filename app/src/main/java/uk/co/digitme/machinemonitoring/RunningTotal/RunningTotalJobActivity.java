package uk.co.digitme.machinemonitoring.RunningTotal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.security.auth.callback.Callback;

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

        int currentQuantity = getIntent().getIntExtra("currentQuantity", 0);
        String buttonText = getResources().getString(R.string.update_total_btn);
        mUpdateTotalButton.setText(buttonText + currentQuantity);

        updateTotalResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    finish();
                });

        long lastUpdateTimestamp = (long) getIntent().getFloatExtra("lastUpdate", 0);
        long currentTimestamp = System.currentTimeMillis() / 1000;
        Log.v("ServerResponseListener", Long.toString(currentTimestamp));
        Log.v("ServerResponseListener", Long.toString(lastUpdateTimestamp));

        Handler h = new Handler();
        Runnable flashUpdateBox = new Runnable() {
            boolean alternate = true;
            @Override
            public void run() {
                if (alternate) {
                    mUpdateTotalButton.setBackgroundColor(Color.RED);
                } else {
                    mUpdateTotalButton.setBackgroundColor(Color.GREEN);
                }
                alternate = !alternate;
                h.postDelayed(this, 1000);
            }
        };
//        TODO This part isnt working right. May be server side, dunno
        long secondsSinceUpdate = currentTimestamp - lastUpdateTimestamp;
        long updateFrequencySeconds = 10;
        long milliSecondsTillUpdateRequest = (updateFrequencySeconds - secondsSinceUpdate) * 1000;
        new CountDownTimer(milliSecondsTillUpdateRequest, 10) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                flashUpdateBox.run();
                mUpdateTotalButton.setText("Update required\nCurrent quantity: " + currentQuantity);
            }
        }.start();


    }


    protected void updateTotal() {
        Intent updateTotalIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
        updateTotalIntent.putExtra("requestCode", JOB_UPDATE_REQUEST_CODE);
        updateTotalIntent.putExtra("url", "/android-update-quantity");
        updateTotalIntent.putExtra("numericalInput", true);
        // Pass the requested data from the initial intent
        updateTotalIntent.putExtra("requestedData", getIntent().getStringExtra("requestedDataOnEnd"));
        // The text shown on the send button
        updateTotalIntent.putExtra("sendButtonText", "Update Total");

        updateTotalResult.launch(updateTotalIntent);
    }



}
