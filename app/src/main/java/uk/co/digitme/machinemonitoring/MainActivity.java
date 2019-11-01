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

/**
 * The app goes to this screen every time it is opens. This activity contacts the server to find out
 * what state this app should be in (eg no user, job active). This activity then starts the activity
 *
 * If the activity cannot connect to the server, it shows options to retry and change the server ip
 */


public class MainActivity extends AppCompatActivity {

    public static final String DEFAULT_URL = "172.23.167.175";

    public static final int REQUEST_LOGIN = 9000;
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
                            switch (state) {
                                // Depending on the state, launch the corresponding activity
                                case "no_user":
                                    // There is no user logged in on this device.
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
                                    }
                                    loginIntent.putExtra("machineText", machineText);
                                    loginIntent.putExtra("IP", IP);
                                    startActivity(loginIntent);
                                    break;
                                case "no_job":
                                    // There is no active job on this device/machine
                                    // Launch the "start new job" activity
                                    Log.d(TAG, "State: no job");
                                    Intent jobInfoIntent = new Intent(getApplicationContext(), JobInfoActivity.class);
                                    startActivity(jobInfoIntent);
                                    break;
                                case "active_job":
                                    // There is an active job, running on this device/machine
                                    // Launch the job in progress activity
                                    String currentActivity;
                                    Boolean setting;
                                    try {
                                        jobNumber = response.getString("wo_number");
                                        colour = response.getString("colour");
                                        currentActivity = response.getString("current_activity");

                                    } catch (JSONException je){
                                        // Replace with default values to prevent crash
                                        jobNumber = "";
                                        colour="#ffffff";
                                        currentActivity = "uptime";
                                    }
                                    Log.d(TAG, "State: job active, Job:" + jobNumber);
                                    Intent activeJobIntent = new Intent(getApplicationContext(), JobInProgressActivity.class);
                                    // The activity requires possible downtime reasons to populate a dropdown
                                    ArrayList<String> codes = new ArrayList<>();
                                    codes = parseJsonList(response, "activity_codes");
                                    activeJobIntent.putExtra("activityCodes", codes);
                                    // Send the current activity to set the spinner on
                                    activeJobIntent.putExtra("currentActivity",  currentActivity);
                                    // The activity shows the job number on the action bar
                                    activeJobIntent.putExtra("jobNumber", jobNumber);
                                    // The activity's background changes depending on the activity
                                    activeJobIntent.putExtra("colour", colour);
                                    startActivity(activeJobIntent);
                                    break;
                                case "setting":
                                    try {
                                        jobNumber = response.getString("wo_number");
                                        colour = response.getString("colour");
                                    } catch (JSONException je){
                                        // Replace with default values to prevent crash
                                        jobNumber = "";
                                        colour="#ffffff";
                                    }
                                    Log.d(TAG, "State: setting, Job:" + jobNumber);
                                    Intent settingIntent = new Intent(getApplicationContext(), SettingInProgress.class);
                                    // The activity shows the job number on the action bar
                                    settingIntent.putExtra("jobNumber", jobNumber);
                                    // The activity's background changes depending on the activity
                                    settingIntent.putExtra("colour", colour);
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
                mStatusText.setText("e.getMessage()");
                mStatusText.setVisibility(View.VISIBLE);
                mRetryButton.setVisibility(View.VISIBLE);
                mSetAddressButton.setVisibility(View.VISIBLE);
            }
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
