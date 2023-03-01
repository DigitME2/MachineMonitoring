package uk.co.digitme.machinemonitoring.CustomFlow1;

import static uk.co.digitme.machinemonitoring.MainActivity.TAG;
import static uk.co.digitme.machinemonitoring.Pausable.PausableWorkflow.STATE_JOB_PAUSED;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.json.JsonArray;

import uk.co.digitme.machinemonitoring.Default.DefaultJobActivity;
import uk.co.digitme.machinemonitoring.Default.Workflow;
import uk.co.digitme.machinemonitoring.Pausable.JobPausedActivity;

public class Custom1Workflow extends Workflow {
    public Custom1Workflow(Context context, JSONObject serverResponse) {
        super(context, serverResponse);
    }

    @Override
    public Intent getIntent() throws JSONException {
        String state = super.getState();
        Intent intent;
        Intent intent_template;
        switch (state) {
            // Depending on the state, launch the corresponding activity
            case STATE_NO_USER:
                intent = super.noUserFlow();
                break;

            case STATE_NO_JOB:
                intent = super.noJobFlow();
                break;

            case STATE_JOB_PAUSED:
                intent_template = custom1PausedFlow();
                intent = new Intent(super.context, Custom1PausedActivity.class);
                intent.putExtras(intent_template);
                break;

            case STATE_JOB_ACTIVE:
                intent = custom1JobFlow();
                break;


            default:
                // If the state is not understood, tell the user and show the
                // buttons to retry/change server address
                Log.e(TAG, "State could not be parsed: " + state);
                throw new JSONException("Could not parse state");
        }

        return intent;
    }

    private Intent custom1JobFlow() throws JSONException {
        /// Use the superclass to get the default intent and reuse the extras
        Intent defaultIntent = super.activeJobFlow();
        Intent intent = new Intent(super.context, Custom1JobActivity.class);
        intent.putExtras(defaultIntent);
        intent.putExtra("updateFrequency", Integer.parseInt(serverResponse.getString("update_frequency")));
        intent.putExtra("lastUpdate", Double.parseDouble(serverResponse.getString("last_update")));
        intent.putExtra("currentQuantity", serverResponse.getInt("current_quantity"));
        return intent;
    }

    protected Intent custom1PausedFlow() throws JSONException {
        // There is an active job, running on this device/machine, launch the job in progress activity
        int machineId;
        String jobNumber;
        int currentActivityCodeId;
        JSONArray componentsJson;
        String categoriesString;
        String activityCodesString;
        JSONObject requestedDataOnEnd;

        // Get additional data from the response, to pass to the next activity
        machineId = serverResponse.getInt("machine_id");
        jobNumber = serverResponse.getString("job_number");
        currentActivityCodeId = serverResponse.getInt("current_activity_code_id");
        requestedDataOnEnd = serverResponse.getJSONObject("requested_data_on_end");
        activityCodesString = serverResponse.getJSONArray("activity_codes").toString();
        componentsJson = serverResponse.getJSONArray("components");
        ArrayList<String> componentsList = new ArrayList<>(toStringArrayList(componentsJson));
        categoriesString = serverResponse.getJSONArray("categories").toString();

        Intent activeJobIntent = new Intent(context, DefaultJobActivity.class);
        activeJobIntent.putExtra("machineId", machineId);
        activeJobIntent.putExtra("components", componentsList);
        activeJobIntent.putExtra("categories", categoriesString);
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

    public static ArrayList<String> toStringArrayList(JSONArray array) {
        ArrayList<String> list = new ArrayList<>();
        for(int i = 0; i < array.length(); i++){
            try {
                list.add(array.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

}
