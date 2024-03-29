package uk.co.digitme.machinemonitoring;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;

import tech.gusavila92.websocketclient.WebSocketClient;
import uk.co.digitme.machinemonitoring.Default.ActivityCode;
import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.LoggedInActivity;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;


/**
 * The base activity for the job in progress screen
 */
public abstract class JobActivityBase extends LoggedInActivity {

    final String TAG = "JobInProgressActivity";
    public static final int JOB_END_DATA_REQUEST_CODE = 9002;
    public Spinner activityCodeSpinner;
    Button mEndJobButton;
    public ActivityResultLauncher<Intent> endJobResult;

    String jobNumber;
    public ArrayList<ActivityCode> activityCodes = new ArrayList<>();
    int machineId = 0;

    public DbHelper dbHelper;
    public URI webSocketUri;
    public String updateUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        updateUrl = dbHelper.getServerAddress() + "/android-update";

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // Set the action bar to read the job number
        jobNumber = getIntent().getStringExtra("jobNumber");
        String actionBarTitle = "Job in Progress";
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(actionBarTitle);
            if (jobNumber != null) {
                ab.setSubtitle(jobNumber);
            }
        }
        machineId = getIntent().getIntExtra("machineId", 0);

        // Set up the end job button
        mEndJobButton = findViewById(R.id.end_job_button);
        if (mEndJobButton != null) {
            mEndJobButton.setOnClickListener(new OnOneOffClickListener() {
                @Override
                public void onSingleClick(View view) {
                    endJob();
                }
            });
        }

        endJobResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> finish());

        // Parse the activity codes into an array of ActivityCode objects
        try {
            JSONArray jsonActivityCodes = new JSONArray(getIntent().getStringExtra("activityCodes"));
            int len = jsonActivityCodes.length();
            for (int i = 0; i < len; i++) {
                JSONObject jso = (JSONObject) jsonActivityCodes.get(i);
                activityCodes.add(new ActivityCode(jso));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ArrayAdapter<ActivityCode> activityCodeAdapter = new ArrayAdapter<> (this, R.layout.spinner_item, activityCodes);

        // Set up the spinner for the downtime codes
        activityCodeSpinner = findViewById(R.id.activity_code_spinner);
        if (activityCodeSpinner != null) {
            if (activityCodes != null) {
                activityCodeAdapter.setDropDownViewResource(R.layout.spinner_item);
                activityCodeSpinner.setAdapter(activityCodeAdapter);
                //Set the spinner to show the current activity code
                int current_activity_code_id = getIntent().getIntExtra("currentActivityCodeId", 0);
                setActivityCodeSpinner(current_activity_code_id);
            }
            // As soon as the reason is changed, tell the server
            SpinnerInteractionListener listener = new SpinnerInteractionListener();
            activityCodeSpinner.setOnTouchListener(listener);
            activityCodeSpinner.setOnItemSelectedListener(listener);
        }
        try {
            webSocketUri = dbHelper.getWebsocketUpdatesURI();
        } catch (URISyntaxException e) {
            Toast.makeText(getApplicationContext(), "Could not parse server URI", Toast.LENGTH_LONG).show();
        }
    }

    public class SpinnerInteractionListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

        boolean userSelect = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            v.performClick();
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
            if (userSelect) {
                // Your selection handling code here
                ActivityCode ac = (ActivityCode) adapterView.getItemAtPosition(pos);
                View rootView = getWindow().getDecorView().getRootView();
                rootView.setBackgroundColor(Color.parseColor(ac.colour));
                Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(Color.parseColor(ac.colour)));
                updateActivity(ac.activityCodeId);
                userSelect = false;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            // pass
        }

    }


    public void setActivityCodeSpinner(int activityCodeId) {
        int len = activityCodes.size();
        for(int i = 0; i < len; i++){
            ActivityCode ac = activityCodes.get(i);
            if (ac.activityCodeId == activityCodeId){
                activityCodeSpinner.post(() -> activityCodeSpinner.setSelection(activityCodes.indexOf(ac)));
                View rootView = getWindow().getDecorView().getRootView();
                rootView.setBackgroundColor(Color.parseColor(ac.colour));
                Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(Color.parseColor(ac.colour)));
            }
        }
    }


    /**
     * Launches a data input activity to add additional data and then ends the activity
     */
    public void endJob(){
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
    public void updateActivity(int activityCodeId){

        // Contact the server to inform of the update
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            JSONObject jsonBody = new JSONObject();

            jsonBody.put("device_uuid", dbHelper.getDeviceUuid());
            jsonBody.put("activity_code_id", activityCodeId);
            // Send the request. Don't listen for the response and ignore any failures, this isn't a critical update.
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    updateUrl,
                    jsonBody,
                    response -> {
                        boolean success;
                        try {
                            // Get the state from the server response
                            success = response.getBoolean("success");
                            Log.d(TAG, "Server response: " + response);
                            if (!success){
                                finish();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.v(TAG, "Failed parsing server response: " + response);
                            finish();
                        }
                    },
                    error -> {
                        Log.v("ErrorListener", String.valueOf(error));
                        Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                        finish();
                    });

            Log.d(TAG, "POSTing to " + updateUrl);
            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
                finish();
            }
        }
    }

    public class OeeWebSocketClient extends WebSocketClient {

        public OeeWebSocketClient() {
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
            runOnUiThread(() -> {
                try {
                    JSONObject websocketMessage = new JSONObject(message);
                    String action = (String) websocketMessage.get("action");
                    if (action.equals("activity_change")){
                        int newActivityCodeId = websocketMessage.getInt("activity_code_id");
                        setActivityCodeSpinner(newActivityCodeId);
                    }
                    else if (action.equals("logout")){
                        finish();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
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

    public void createWebSocketClient(WebSocketClient webSocketClient) {
        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(0);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }
}

