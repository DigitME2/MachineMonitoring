package uk.co.digitme.machinemonitoring.Pausable;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;

import tech.gusavila92.websocketclient.WebSocketClient;
import uk.co.digitme.machinemonitoring.Default.ActivityCode;
import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.EndActivityResponseListener;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.JobActivityBase;
import uk.co.digitme.machinemonitoring.R;

/**
 * The activity shown when the server reports this device's job is paused
 */
public class JobPausedActivity extends JobActivityBase {

    final String TAG = "JobPausedActivity";

    Button mResumeButton;
    EditText mNotes;

    String jobNumber;

    DbHelper dbHelper;
    public URI webSocketUri;
    private int machineId;
    private int currentActivityId;

    PausedJobWebSocketClient pausedJobWebSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.pausable_activity_job_paused);
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        // This will send updates to a different route when the spinner is selected
        updateUrl = dbHelper.getServerAddress() + "/pausable-android-update";
        // Stop the screen timeout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        currentActivityId = getIntent().getIntExtra("currentActivityCodeId", 0);
        setBackgroundColour(currentActivityId);

        machineId = getIntent().getIntExtra("machineId", 0);
        // Set the action bar to read the job number
        jobNumber = getIntent().getStringExtra("jobNumber");
        Log.v(TAG, "Job number: " + jobNumber);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Job in progress: " + jobNumber);

        mResumeButton = findViewById(R.id.resume_button);
        mNotes = findViewById(R.id.notes);

        mResumeButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                resumeJob();
            }
        });

        try {
            webSocketUri = dbHelper.getWebsocketUpdatesURI();
        } catch (URISyntaxException e) {
            Toast.makeText(getApplicationContext(), "Could not parse server URI", Toast.LENGTH_LONG).show();
        }
        pausedJobWebSocketClient = new PausedJobWebSocketClient();
        pausedJobWebSocketClient.setConnectTimeout(10000);
        pausedJobWebSocketClient.setReadTimeout(60000);
        pausedJobWebSocketClient.enableAutomaticReconnection(5000);
        pausedJobWebSocketClient.connect();


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


    /**
     * Contacts the server indicating the job has been resumed, and ends this activity
     */
    private void resumeJob(){
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = dbHelper.getServerAddress() + "/pausable-resume-job";
            JSONObject jsonBody = new JSONObject();

            jsonBody.put("device_uuid", dbHelper.getDeviceUuid());
            jsonBody.put("downtime_reason", activityCodeSpinner.getSelectedItem().toString());
            jsonBody.put("notes", mNotes.getText().toString());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonBody,
                    new EndActivityResponseListener(this),
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
            int spinnerId = (int) activityCodeSpinner.getSelectedItemId();
            String colour = colours.get(spinnerId);
            // Set the background colour
            View rootView = getWindow().getDecorView().getRootView();
            rootView.setBackgroundColor(Color.parseColor(colour));
        } catch (Exception e){
            Log.e(TAG, e.toString());
        }
    }

public class PausedJobWebSocketClient extends WebSocketClient {

    public PausedJobWebSocketClient() {
        super(webSocketUri);
    }

    @Override
    public void onOpen() {
        Log.i(TAG, "websocket connected");
        JSONObject uuidResponse = new JSONObject();
        try {
            uuidResponse.put("device_uuid", dbHelper.getDeviceUuid());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.send(uuidResponse.toString());
    }

    @Override
    public void onTextReceived(String message) {
        Log.i(TAG, "websocket message: " + message);
        if (!message.equals(String.valueOf(currentActivityId))) {
            finish();
        }
    }

    @Override
    public void onBinaryReceived(byte[] data) {
        Log.i(TAG, "onBinaryReceived");
    }

    @Override
    public void onPingReceived(byte[] data) {
        Log.v(TAG, "onPingReceived");
    }

    @Override
    public void onPongReceived(byte[] data) {
        Log.v(TAG, "onPongReceived");
    }

    @Override
    public void onException(Exception e) {
        Log.e(TAG, "WebSocket Exception");
        Log.e(TAG, e.getMessage());
    }

    @Override
    public void onCloseReceived() {
        Log.w(TAG, "Websocket Closing");
    }
}

}