package uk.co.digitme.machinemonitoring.Default;

import static uk.co.digitme.machinemonitoring.MainActivity.TAG;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import uk.co.digitme.machinemonitoring.DataEntryActivity;
import uk.co.digitme.machinemonitoring.LoginActivity;

public class Workflow {

    public static final String STATE_NO_USER = "no_user";
    public static final String STATE_NO_JOB = "no_job";
    public static final String STATE_JOB_ACTIVE = "active_job";


    public Context context;
    public JSONObject serverResponse;

    public Workflow(Context context, JSONObject serverResponse) {
        this.context = context;
        this.serverResponse = serverResponse;
    }

    public String getState() throws JSONException {
            String state = serverResponse.getString("state");
            Log.d(TAG, "Response: " + serverResponse.toString());
            return state;
    }

    /**
     * Analyses the response from the server and returns the intent that should be launched
     */
    public Intent getIntent() throws JSONException {
        String state = getState();
        Intent intent;

        switch (state){

            case STATE_NO_USER:
                intent = noUserFlow();
                break;

            case STATE_NO_JOB:
                intent = noJobFlow();
                break;

            case STATE_JOB_ACTIVE:
                intent = activeJobFlow();
                break;

            default:
                // If the state is not understood, tell the user and show the
                // buttons to retry/change server address
                Log.e(TAG, "State could not be parsed: " + state);
                throw new JSONException("Could not parse state");
        }

        return intent;
    }

    protected Intent noUserFlow() throws JSONException {
        // Launch the login screen
        Log.d(TAG, "State: no user");
        Intent loginIntent = new Intent(context, LoginActivity.class);
        String machineText = "Could not get assigned machine";
        String IP = "";
        if (serverResponse.has("ip")) {
            IP = serverResponse.getString("ip");
        }
        if (serverResponse.has("machine")) {
            machineText = serverResponse.getString("machine");
        }
        loginIntent.putExtra("machineText", machineText);
        loginIntent.putExtra("IP", IP);
        return loginIntent;
    }

    protected Intent noJobFlow() throws JSONException {
        // Get the requested data from the server, so we know what data to get from the user
        JSONObject requestedData;
        requestedData = serverResponse.getJSONObject("requested_data");
        // Create the intent
        Intent jobInfoIntent = new Intent(context, DataEntryActivity.class);
        // The URL tells the data input activity which URL to post its results to
        jobInfoIntent.putExtra("url", "/android-start-job");
        // If True, the data input activity will show a custom numpad
        jobInfoIntent.putExtra("numericalInput", true);
        // The data to be entered in the activity
        jobInfoIntent.putExtra("requestedData", requestedData.toString());
        // The text shown on the send button
        jobInfoIntent.putExtra("sendButtonText", "Start New Job");
        // The action bar text
        String machineName = serverResponse.getString("machine_name");
        String userName = serverResponse.getString("user_name");
        jobInfoIntent.putExtra("actionBarTitle", machineName);
        jobInfoIntent.putExtra("actionBarSubtitle", userName + " - No Job Active");

        Log.d(TAG, "State: no job");
        return jobInfoIntent;
    }

    protected Intent activeJobFlow() throws JSONException {
        // There is an active job, running on this device/machine, launch the job in progress activity
        int machineId;
        String jobNumber;
        int currentActivityCodeId;
        String activityCodesString;
        JSONObject requestedDataOnEnd;

        // Get additional data from the response, to pass to the next activity
        machineId = serverResponse.getInt("machine_id");
        jobNumber = serverResponse.getString("wo_number");
        currentActivityCodeId = serverResponse.getInt("current_activity_code_id");
        requestedDataOnEnd = serverResponse.getJSONObject("requested_data_on_end");
        activityCodesString = serverResponse.getJSONArray("activity_codes").toString();

        Intent activeJobIntent = new Intent(context, DefaultJobActivity.class);
        activeJobIntent.putExtra("machineId", machineId);
        // The activity requires possible downtime reasons to populate a dropdown
        activeJobIntent.putExtra("activityCodes", activityCodesString);
        // Send the current activity to set the spinner on
        activeJobIntent.putExtra("currentActivityCodeId", currentActivityCodeId);
        // The activity shows the job number on the action bar
        activeJobIntent.putExtra("jobNumber", jobNumber);
        // The data that the server wants from the user when ending a job. This will be passed along to the next activity
        activeJobIntent.putExtra("requestedDataOnEnd", requestedDataOnEnd.toString());
        Log.d(TAG, "State: job active, Job:" + jobNumber);
        return activeJobIntent;
    }

    /**
     * Extract a list of strings from a list in a JSONObject
     *
     * @param json     The JSON object to be parsed
     * @param listName The name of the list to be extracted from the json
     * @return The list as an ArrayList<String>
     */
    public static ArrayList<String> parseJsonList(JSONObject json, String listName) throws JSONException {
        ArrayList<String> reasons = new ArrayList<>();
        JSONArray jsonArray;
        jsonArray = json.getJSONArray(listName);
        int len = jsonArray.length();
        for (int i = 0; i < len; i++) {
            reasons.add(jsonArray.get(i).toString());

        }
        return reasons;
    }

}
