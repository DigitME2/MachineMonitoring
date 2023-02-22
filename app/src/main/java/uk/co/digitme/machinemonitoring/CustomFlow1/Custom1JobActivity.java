package uk.co.digitme.machinemonitoring.CustomFlow1;

import static uk.co.digitme.machinemonitoring.RunningTotal.RunningTotalJobActivity.JOB_UPDATE_REQUEST_CODE;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import uk.co.digitme.machinemonitoring.DataEntryActivity;
import uk.co.digitme.machinemonitoring.Default.ActivityCode;
import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.EndActivityResponseListener;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.JobActivityBase;
import uk.co.digitme.machinemonitoring.Pausable.JobPausedActivity;
import uk.co.digitme.machinemonitoring.Pausable.PausableJobActivity;
import uk.co.digitme.machinemonitoring.R;


/**
 * The activity shown when the server reports this device's job is in progress
 */
public class Custom1JobActivity extends JobActivityBase {
    final String TAG = "Custom1JobActivity";

    Button mUpdateTotalButton;
    ActivityResultLauncher<Intent> updateTotalResult;
    Runnable flashUpdateBox;
    int currentQuantity;

    long lastUpdateTimestampSeconds;

    Button mPauseJobButton;
    DbHelper dbHelper;

    int currentActivityId;

    PausableOeeWebSocketClient pausedOeeWebSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.custom1_activity_job_in_progress);
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
            }

            public void onFinish() {
                flashUpdateBox.run();
                String updateButtonText = "Update required\nCurrent quantity: " + currentQuantity;
                mUpdateTotalButton.setText(updateButtonText);
            }
        }.start();

        currentActivityId = getIntent().getIntExtra("currentActivityCodeId", 0);
        setBackgroundColour(currentActivityId);
        dbHelper = new DbHelper(getApplicationContext());

        mPauseJobButton = findViewById(R.id.pause_job_button);
        mPauseJobButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                pauseJob();
            }
        });
        pausedOeeWebSocketClient = new PausableOeeWebSocketClient();
        createWebSocketClient(pausedOeeWebSocketClient);

    }

    protected void updateTotal() {
        Intent updateTotalIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
        updateTotalIntent.putExtra("requestCode", JOB_UPDATE_REQUEST_CODE);
        updateTotalIntent.putExtra("url", "/android-update-quantity");
        updateTotalIntent.putExtra("numericalInput", true);
        // Create the instructions text
        SimpleDateFormat formatter = new SimpleDateFormat("kk:mm", Locale.ENGLISH);
        String timeString = formatter.format(new Date(lastUpdateTimestampSeconds*1000));
        updateTotalIntent.putExtra("instructionText", "Enter the quantity produced since " + timeString + ".\nCurrent total: " + currentQuantity);
        // Pass the requested data from the initial intent
        updateTotalIntent.putExtra("requestedData", getIntent().getStringExtra("requestedDataOnEnd"));
        // The text shown on the send button
        updateTotalIntent.putExtra("sendButtonText", "Update Total");

        updateTotalResult.launch(updateTotalIntent);
    }

    /**
     * Launches a data input activity to add additional data and then ends the activity.
     * This override adds instructions to the top so we can specify the time of the last update
     */
    @Override
    public void endJob(){
        Intent endJobInfoIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
        endJobInfoIntent.putExtra("requestCode", JOB_END_DATA_REQUEST_CODE);
        endJobInfoIntent.putExtra("url", "/android-end-job");
        endJobInfoIntent.putExtra("numericalInput", true);
        // Pass the requested data from the initial intent
        endJobInfoIntent.putExtra("requestedData", getIntent().getStringExtra("requestedDataOnEnd"));
        // The text shown on the send button
        endJobInfoIntent.putExtra("sendButtonText", "End");
        // Create the instructions text
        SimpleDateFormat formatter = new SimpleDateFormat("kk:mm", Locale.ENGLISH);
        String timeString = formatter.format(new Date(lastUpdateTimestampSeconds*1000));
        endJobInfoIntent.putExtra("instructionText", "Enter the quantity produced since " + timeString + ".\nCurrent total: " + currentQuantity);

        endJobResult.launch(endJobInfoIntent);
    }

    private void pauseJob(){
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = dbHelper.getServerAddress() + "/pausable-pause-job";
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("device_uuid", dbHelper.getDeviceUuid());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonRequest,
                    new EndActivityResponseListener(this),
                    error -> {
                        Log.v("ErrorListener", String.valueOf(error));
                        Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                    });

            Log.d(TAG, "POSTing to " + url);
            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public class PausableOeeWebSocketClient extends OeeWebSocketClient {

        public PausableOeeWebSocketClient() {
            super();
        }

        @Override
        public void onTextReceived(String message) {
            Log.i(TAG, "websocket message: " + message);
            if (!message.equals(String.valueOf(currentActivityId))) {
                finish();
            }
        }

    }

    private void setBackgroundColour(int activityCodeId){
        int len = activityCodes.size();
        for(int i = 0; i < len; i++){
            ActivityCode ac = activityCodes.get(i);
            if (ac.activityCodeId == activityCodeId){
                View rootView = getWindow().getDecorView().getRootView();
                rootView.setBackgroundColor(Color.parseColor(ac.colour));
                Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(Color.parseColor(ac.colour)));
            }
        }
    }


}
