package uk.co.digitme.machinemonitoring.Pausable;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.Objects;

import uk.co.digitme.machinemonitoring.Default.ActivityCode;
import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.EndActivityResponseListener;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.JobActivityBase;
import uk.co.digitme.machinemonitoring.R;


/**
 * The activity shown when the server reports this device's job is in progress
 */
public class PausableJobActivity extends JobActivityBase {

    final String TAG = "JobInProgressActivity";

    Button mPauseJobButton;
    DbHelper dbHelper;

    int currentActivityId;

    PausableOeeWebSocketClient pausedOeeWebSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.pausable_activity_job_in_progress);
        super.onCreate(savedInstanceState);
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

    /**
     * Contacts the server, indicating a job pause has been requested, then finishes this activity
     */
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