package uk.co.digitme.machinemonitoring;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import uk.co.digitme.machinemonitoring.Default.JobInProgressActivity;
import uk.co.digitme.machinemonitoring.Default.LoginActivity;
import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.Pneumatrol.JobPausedActivity;
import uk.co.digitme.machinemonitoring.Pneumatrol.SettingInProgressActivity;

/**
 * The app goes to this screen every time it is opens. This activity contacts the server to find out
 * what state this app should be in (eg no user, job active). This activity then starts the activity
 *
 * If the activity cannot connect to the server, it shows options to retry and change the server ip
 */


public class MainActivity extends AppCompatActivity {

    public static final String DEFAULT_URL = "172.23.167.175";

    public static final int REQUEST_LOGIN = 9000;
    public static final int REQUEST_START_JOB = 4000;
    public static final int REQUEST_END_JOB = 4001;
    public static final int RESULT_NO_JOB = 8001;

    public static final String TAG = "MainActivity";

    TextView mStatusText;
    Button mRetryButton;
    Button mSetAddressButton;

    DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dbHelper = new DbHelper(getApplicationContext());

        setContentView(R.layout.activity_main);
        //TODO Fix the error that shows up when the server can't be found
        getSupportActionBar().setTitle("");

        // Status text and retry/change server buttons.
        // These will become visible after failing to connect.
        mStatusText = findViewById(R.id.main_activity_status);
        mRetryButton = findViewById(R.id.retry_button);

        // The retry button attempts to contact the server again.
        mRetryButton.setOnClickListener(new OnOneOffClickListener(){
            @Override
            public void onSingleClick(View v) {
                checkState();
            }
        });
        // The "set address" button opens a new activity allowing the user to change the server ip
        mSetAddressButton = findViewById(R.id.set_address_button);
        mSetAddressButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide the buttons by default. This stops them showing during transitions between activities
        mStatusText.setVisibility(View.INVISIBLE);
        mRetryButton.setVisibility(View.INVISIBLE);
        mSetAddressButton.setVisibility(View.INVISIBLE);
        // When arriving at this page, immediately contact the server to see which screen the app
        // should be on, and start that activity
        checkState();
    }

    /**
     * Contacts the server to get the current state of this device e.g. active job, no user
     *
     *  Also launches the corresponding activity
     */
    private void checkState() {

        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + dbHelper.getServerAddress() + "/checkstate";
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                    url,
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            String workflowType;
                            String state;
                            try {
                                // Get the workflow type from the server response
                                workflowType = response.getString("workflow_type");
                                state = response.getString("state");
                                Log.d(TAG, "Response: " + response.toString());
                            } catch (Exception e) {
                                Log.v(TAG, "Failed parsing server response: " + response);
                                mStatusText.setText("Bad server response: " + response);
                                mStatusText.setVisibility(View.VISIBLE);
                                mRetryButton.setVisibility(View.VISIBLE);
                                mSetAddressButton.setVisibility(View.VISIBLE);
                                return;
                            }
                            // If the server shows no user logged in, start the login screen
                            // This occurs regardless of the machine's workflow
                            if (state.equals("no_user")){
                                // Launch the login screen
                                Log.d(TAG, "State: no user");
                                Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
                                String machineText = "Could not get assigned machine";
                                String IP = "";
                                try{
                                    if (response.has("ip")){
                                        IP = response.getString("ip");
                                    }
                                    if (response.has("machine")){
                                        machineText = response.getString("machine");
                                    }

                                } catch (JSONException je){
                                    je.printStackTrace();
                                    mStatusText.setVisibility(View.VISIBLE);
                                    mRetryButton.setVisibility(View.VISIBLE);
                                    mSetAddressButton.setVisibility(View.VISIBLE);
                                }
                                loginIntent.putExtra("machineText", machineText);
                                loginIntent.putExtra("IP", IP);
                                startActivity(loginIntent);
                            }
                            else {
                                // Direct the user differently depending on the workflow type
                                switch (workflowType) {
                                    case "default":
                                        defaultWorkFlow(response);
                                        break;

                                    case "pneumatrol":
                                        pneumatrolWorkflow(response);
                                        break;

                                    case "":

                                }
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // If an error occurs, tell the user and show the
                    // buttons to retry/change server address
                    mStatusText.setVisibility(View.VISIBLE);
                    mRetryButton.setVisibility(View.VISIBLE);
                    mSetAddressButton.setVisibility(View.VISIBLE);
                    // Show a different error message depending on the error
                    if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                        mStatusText.setText("Could not connect to network");
                    } else if (error instanceof ServerError){
                        mStatusText.setText("Could not connect to server");
                    }
                    Log.v("ErrorListener", String.valueOf(error));
                    Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                }
            });

            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null) {
                // Display any error message to the user and reveal the buttons to retry/change server address
                Log.e(TAG, e.getMessage());
                mStatusText.setText(e.getMessage());
                mStatusText.setVisibility(View.VISIBLE);
                mRetryButton.setVisibility(View.VISIBLE);
                mSetAddressButton.setVisibility(View.VISIBLE);
            }
        }

    }

    private void defaultWorkFlow(JSONObject response){
        // Get the state from the server response
        String state;
        try {
            state = response.getString("state");
            Log.d(TAG, "Response: " + response.toString());
        } catch (Exception e) {
            Log.v(TAG, "Failed parsing server response: " + response);
            mStatusText.setText("Bad server response");
            return;
        }
        // Depending on the state, launch the corresponding activity
        switch (state) {
            case "no_job":
                // There is no active job on this device/machine, launch the "start new job" activity

                // Get the requested data from the server, so we know what data to get from the user
                JSONObject requestedData;
                try {
                    requestedData = response.getJSONObject("requested_data");
                } catch (JSONException e) {
                    Log.e(TAG,e.toString());
                    return;
                }
                // Create the intent
                Intent jobInfoIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
                // The URL tells the data input activity which URL to post its results to
                jobInfoIntent.putExtra("url", "/androidstartjob");
                // If True, the data input activity will show a custom numpad
                jobInfoIntent.putExtra("numericalInput", true);
                // The data to be entered in the activity
                jobInfoIntent.putExtra("requestedData", requestedData.toString());
                // The text shown on the send button
                jobInfoIntent.putExtra("sendButtonText", "Start New Job");
                startActivityForResult(jobInfoIntent, REQUEST_START_JOB);
                Log.d(TAG, "State: no job");
                break;

            case "active_job":
                // There is an active job, running on this device/machine, launch the job in progress activity
                String jobNumber;
                String colour;
                String currentActivity;
                ArrayList<String> codes = new ArrayList<>();
                JSONObject requestedDataOnEnd;

                // Get additional data from the response, to pass to the next activity
                try {
                    jobNumber = response.getString("wo_number");
                    colour = response.getString("colour");
                    currentActivity = response.getString("current_activity");
                    requestedDataOnEnd = response.getJSONObject("requested_data_on_end");
                    codes = parseJsonList(response, "activity_codes");

                } catch (Exception e){
                    // Replace with default values to prevent crash
                    Log.e(TAG,e.toString());
                    jobNumber = "";
                    colour="#ffffff";
                    currentActivity = "uptime";
                    requestedDataOnEnd = new JSONObject();
                    codes.add("Failed to get codes");
                }
                Intent activeJobIntent = new Intent(getApplicationContext(), JobInProgressActivity.class);
                // The activity requires possible downtime reasons to populate a dropdown
                activeJobIntent.putExtra("activityCodes", codes);
                // Send the current activity to set the spinner on
                activeJobIntent.putExtra("currentActivity",  currentActivity);
                // The activity shows the job number on the action bar
                activeJobIntent.putExtra("jobNumber", jobNumber);
                // The activity's background changes depending on the activity
                activeJobIntent.putExtra("colour", colour);
                // The data that the server wants from the user when ending a job. This will be passed along to the next activity
                activeJobIntent.putExtra("requestedDataOnEnd", requestedDataOnEnd.toString());
                startActivity(activeJobIntent);

                Log.d(TAG, "State: job active, Job:" + jobNumber);
                break;


            default:
                // If the state is not understood, tell the user and show the
                // buttons to retry/change server address
                Log.e(TAG, "State could not be parsed: " + state);
                mStatusText.setText("Bad server response");
                mStatusText.setVisibility(View.VISIBLE);
                mRetryButton.setVisibility(View.VISIBLE);
                mSetAddressButton.setVisibility(View.VISIBLE);
                break;
        }
    }


    private void pneumatrolWorkflow(JSONObject response){
        String state;
        try {
            // Get the state from the server response
            state = response.getString("state");
            Log.d(TAG, "Response: " + response.toString());
        } catch (Exception e) {
            Log.v(TAG, "Failed parsing server response: " + response);
            mStatusText.setText("Bad server response");
            return;
        }
        String colour;
        String jobNumber;
        String currentActivity;
        JSONObject requestedDataOnEnd;

        switch (state) {
            // Depending on the state, launch the corresponding activity
            case "no_job":
                // There is no active job on this device/machine, launch the "start new job" activity
                // Create the intent
                Intent jobInfoIntent = new Intent(getApplicationContext(), DataEntryActivity.class);
                // Get the requested data from the server, so we know what data to get from the user
                JSONObject requestedData;
                boolean setting;
                try {
                    setting = response.getBoolean("setting");
                    requestedData = response.getJSONObject("requested_data");
                    jobInfoIntent.putExtra("requestedData", requestedData.toString());
                    if (response.has("autofill_data")) {
                        // Add autofill data for the data input boxes to the intent
                        jobInfoIntent.putExtra("requestedDataAutofill", response.getJSONObject("autofill_data").toString());
                    }
                } catch (JSONException e) {
                    Log.e(TAG,e.toString());
                    return;
                }

                // The URL tells the data input activity which URL to post its results to
                // POST to a different URL and show a different button depending on if the job is setting
                if (setting) {
                    jobInfoIntent.putExtra("url", "/pneumatrolstartsetting");
                    jobInfoIntent.putExtra("sendButtonText", "Start Setting");
                }
                else {
                    jobInfoIntent.putExtra("url", "/pneumatrolstartjob");
                    jobInfoIntent.putExtra("sendButtonText", "Start New Job");
                }
                // If True, the data input activity will show a custom numpad
                jobInfoIntent.putExtra("numericalInput", true);




                startActivityForResult(jobInfoIntent, REQUEST_START_JOB);
                Log.d(TAG, "State: no job");
                break;

            case "active_job":
                // There is an active job, running on this device/machine, launch the job in progress activity

                try {
                    jobNumber = response.getString("wo_number");
                    colour = response.getString("colour");
                    currentActivity = response.getString("current_activity");
                    requestedDataOnEnd = response.getJSONObject("requested_data_on_end");
                } catch (JSONException je){
                    // Replace with default values to prevent crash
                    jobNumber = "";
                    colour="#ffffff";
                    currentActivity = "uptime";
                    requestedDataOnEnd = new JSONObject();
                }
                Log.d(TAG, "State: job active, Job:" + jobNumber);
                Intent activeJobIntent = new Intent(getApplicationContext(), uk.co.digitme.machinemonitoring.Pneumatrol.JobInProgressActivity.class);
                // The activity shows the job number on the action bar);
                activeJobIntent.putExtra("jobNumber", jobNumber);
                // Send the current activity to show in the action bar
                activeJobIntent.putExtra("currentActivity",  currentActivity);
                // The activity's background changes depending on the activity
                activeJobIntent.putExtra("colour", colour);
                // The data that the server wants from the user when ending a job. This will be passed along to the next activity
                activeJobIntent.putExtra("requestedDataOnEnd", requestedDataOnEnd.toString());
                startActivity(activeJobIntent);
                break;

            case "paused":
                try {
                    jobNumber = response.getString("wo_number");
                    colour = response.getString("colour");
                } catch (JSONException je){
                    // Replace with default values to prevent crash
                    jobNumber = "";
                    colour="#ffffff";
                }
                Log.d(TAG, "State: setting, Job:" + jobNumber);
                Intent pausedIntent = new Intent(getApplicationContext(), JobPausedActivity.class);
                // The activity shows the job number on the action bar
                pausedIntent.putExtra("jobNumber", jobNumber);
                // The activity's background changes depending on the activity
                pausedIntent.putExtra("colour", colour);
                // The activity requires possible downtime reasons to populate a dropdown
                ArrayList<String> codes = new ArrayList<>();
                codes = parseJsonList(response, "activity_codes");
                pausedIntent.putExtra("activityCodes", codes);
                startActivity(pausedIntent);
                break;

            case "setting":
                try {
                    jobNumber = response.getString("wo_number");
                    colour = response.getString("colour");
                    requestedDataOnEnd = response.getJSONObject("requested_data_on_end");
                } catch (JSONException je){
                    // Replace with default values to prevent crash
                    jobNumber = "";
                    colour="#ffffff";
                    requestedDataOnEnd = new JSONObject();
                }
                Log.d(TAG, "State: setting, Job:" + jobNumber);
                Intent settingIntent = new Intent(getApplicationContext(), SettingInProgressActivity.class);
                // The activity shows the job number on the action bar
                settingIntent.putExtra("jobNumber", jobNumber);
                // The activity's background changes depending on the activity
                settingIntent.putExtra("colour", colour);
                // The data that the server wants from the user when ending a job. This will be passed along to the next activity
                settingIntent.putExtra("requestedDataOnEnd", requestedDataOnEnd.toString());
                startActivity(settingIntent);
                break;

            default:
                // If the state is not understood, tell the user and show the
                // buttons to retry/change server address
                Log.e(TAG, "State could not be parsed: " + state);
                mStatusText.setText("Bad server response");
                mStatusText.setVisibility(View.VISIBLE);
                mRetryButton.setVisibility(View.VISIBLE);
                mSetAddressButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * Extract a list of strings from a list in a JSONObject
     *
     * @param json The JSON object to be parsed
     * @param listName The name of the list to be extracted from the json
     * @return The list as an ArrayList<String>
     */
    private ArrayList<String> parseJsonList(JSONObject json, String listName){
        ArrayList<String> reasons = new ArrayList<>();
        JSONArray jsonArray;
        try {
            jsonArray = json.getJSONArray(listName);
            int len = jsonArray.length();
            for (int i=0;i<len;i++){
                reasons.add(jsonArray.get(i).toString());

            }

        }catch (JSONException je){
            Log.v(TAG, je.toString());
            return null;
        }

        return reasons;
    }
}
