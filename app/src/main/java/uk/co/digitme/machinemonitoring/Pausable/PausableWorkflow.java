package uk.co.digitme.machinemonitoring.Pausable;

import static uk.co.digitme.machinemonitoring.MainActivity.TAG;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import uk.co.digitme.machinemonitoring.Default.Workflow;

public class PausableWorkflow extends Workflow {
    public static final String STATE_JOB_PAUSED = "paused";


    public PausableWorkflow(Context context, JSONObject serverResponse) {
        super(context, serverResponse);
    }

    @Override
    public Intent getIntent() throws JSONException {
        String state = super.getState();
        Intent intent;
        switch (state) {
            // Depending on the state, launch the corresponding activity
            case STATE_NO_USER:
                intent = super.noUserFlow();
                break;

            case STATE_NO_JOB:
                intent = super.noJobFlow();
                break;

            case STATE_JOB_ACTIVE:
                intent = activePausableJobFlow();
                break;

            case STATE_JOB_PAUSED:
                intent = pausedJobFlow();
                break;

            default:
                // If the state is not understood, tell the user and show the
                // buttons to retry/change server address
                Log.e(TAG, "State could not be parsed: " + state);
                throw new JSONException("Could not parse state");
        }

        return intent;
    }

    public Intent activePausableJobFlow() throws JSONException {
        // There is an active job, running on this device/machine, launch the job in progress activity
        String jobNumber;
        String colour;
        String currentActivity;
        JSONObject requestedDataOnEnd;

        // Get additional data from the response, to pass to the next activity
        jobNumber = serverResponse.getString("wo_number");
        colour = serverResponse.getString("colour");
        currentActivity = serverResponse.getString("current_activity");
        requestedDataOnEnd = serverResponse.getJSONObject("requested_data_on_end");

        Intent activeJobIntent = new Intent(context, JobInProgressActivity.class);
        // Send the current activity to set the spinner on
        activeJobIntent.putExtra("currentActivity", currentActivity);
        // The activity shows the job number on the action bar
        activeJobIntent.putExtra("jobNumber", jobNumber);
        // The activity's background changes depending on the activity
        activeJobIntent.putExtra("colour", colour);
        // The data that the server wants from the user when ending a job. This will be passed along to the next activity
        activeJobIntent.putExtra("requestedDataOnEnd", requestedDataOnEnd.toString());
        Log.d(TAG, "State: job active, Job:" + jobNumber);
        return activeJobIntent;
    }

    public Intent pausedJobFlow() throws JSONException {
        String jobNumber = super.serverResponse.getString("wo_number");
        String colour = super.serverResponse.getString("colour");
        Log.d(TAG, "State: setting, Job:" + jobNumber);
        Intent pausedIntent = new Intent(super.context, JobPausedActivity.class);
        // The activity shows the job number on the action bar
        pausedIntent.putExtra("jobNumber", jobNumber);
        // The activity's background changes depending on the activity
        pausedIntent.putExtra("colour", colour);
        // The activity requires possible downtime reasons to populate a dropdown
        ArrayList<String> codes;
        codes = parseJsonList(super.serverResponse, "activity_codes");
        pausedIntent.putExtra("activityCodes", codes);
        return pausedIntent;
    }
}
