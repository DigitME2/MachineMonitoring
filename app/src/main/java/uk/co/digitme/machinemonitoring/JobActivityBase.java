package uk.co.digitme.machinemonitoring;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
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

    Spinner activityCodeSpinner;
    Button mEndJobButton;
    public ActivityResultLauncher<Intent> endJobResult;

    String jobNumber;
    ArrayList<ActivityCode> activityCodes = new ArrayList<>();
    int machineId = 0;

    public DbHelper dbHelper;

    private WebSocketClient webSocketClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Change the colour of the background. The colour is sent by the server
//        String colour = getIntent().getStringExtra("colour");
//        View rootView = getWindow().getDecorView().getRootView();
//        rootView.setBackgroundColor(Color.parseColor(colour));

        // Set the action bar to read the job number
        jobNumber = getIntent().getStringExtra("jobNumber");
        String actionBarTitle = "Job in Progress";
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            // Set the colour of the action bar to match the background
//            ab.setBackgroundDrawable(new ColorDrawable(Color.parseColor(colour)));
            ab.setTitle(actionBarTitle);
            if (jobNumber != null) {
                ab.setSubtitle(jobNumber);
            }
        }
        machineId = getIntent().getIntExtra("machineId", 0);

        // Set up the end job button
        mEndJobButton = findViewById(R.id.end_job_button);
        mEndJobButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                endJob();
            }
        });

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

        ArrayAdapter<ActivityCode> activityCodeAdapter = new ArrayAdapter<ActivityCode> (this, R.layout.spinner_item, activityCodes);

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
            activityCodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                // Set a count to ignore the first change, which happens during the creation of the activity
                int count = 0;

                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    ActivityCode ac = (ActivityCode) adapterView.getItemAtPosition(i);
                    if (count > 0) {
                        View rootView = getWindow().getDecorView().getRootView();
                        rootView.setBackgroundColor(Color.parseColor(ac.colour));
                        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(Color.parseColor(ac.colour)));
                        updateActivity(ac.activityCodeId);
                    }
                    count++;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // Do nothing
                }
            });
        }

        createWebSocketClient();

    }

    public void setActivityCodeSpinner(int activityCodeId) {
        int len = activityCodes.size();
        for(int i = 0; i < len; i++){
            ActivityCode ac = activityCodes.get(i);
            if (ac.activityCodeId == activityCodeId){
                activityCodeSpinner.setSelection(activityCodes.indexOf(ac));
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
    private void updateActivity(int activityCodeId){

        // Contact the server to inform of the update
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = dbHelper.getServerAddress() + "/android-update";
            JSONObject jsonBody = new JSONObject();

            jsonBody.put("activity_code_id", activityCodeId);
            // Send the request. Don't listen for the response and ignore any failures, this isn't a critical update.
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
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

            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
                finish();
            }
        }
    }




    private void createWebSocketClient() {
        URI uri;
        try {
            uri = new URI("ws://192.168.0.100:80/activity-updates");
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                JSONObject machineIdResponse = new JSONObject();
                try {
                    machineIdResponse.put("machine_id", machineId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                webSocketClient.send(machineIdResponse.toString());
            }

            @Override
            public void onTextReceived(String message) {
                System.out.println("onTextReceived");
                System.out.println(message);
//                setActivityCodeSpinner(Integer.parseInt(message));
            }

            @Override
            public void onBinaryReceived(byte[] data) {
                System.out.println("onBinaryReceived");
            }

            @Override
            public void onPingReceived(byte[] data) {
                System.out.println("onPingReceived");
            }

            @Override
            public void onPongReceived(byte[] data) {
                System.out.println("onPongReceived");
            }

            @Override
            public void onException(Exception e) {
                System.out.println("onException");
                System.out.println(e.getMessage());
            }

            @Override
            public void onCloseReceived() {
                System.out.println("onCloseReceived");
            }
        };

        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.addHeader("Origin", "http://developer.example.com");
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }
}
