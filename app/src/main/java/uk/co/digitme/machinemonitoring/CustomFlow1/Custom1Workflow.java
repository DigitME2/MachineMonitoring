package uk.co.digitme.machinemonitoring.CustomFlow1;

import static uk.co.digitme.machinemonitoring.MainActivity.TAG;
import static uk.co.digitme.machinemonitoring.Pausable.PausableWorkflow.STATE_JOB_PAUSED;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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
                intent_template = super.activeJobFlow();
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
}
