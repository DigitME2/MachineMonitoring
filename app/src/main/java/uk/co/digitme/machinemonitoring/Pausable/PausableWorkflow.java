package uk.co.digitme.machinemonitoring.Pausable;

import static uk.co.digitme.machinemonitoring.MainActivity.TAG;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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
        Intent intent_template;
        switch (state) {
            // Depending on the state, launch the corresponding activity
            case STATE_NO_USER:
                intent = super.noUserFlow();
                break;

            case STATE_NO_JOB:
                intent = super.noJobFlow();
                break;

            case STATE_JOB_ACTIVE:
                intent_template = super.activeJobFlow();
                intent = new Intent(super.context, PausableJobActivity.class);
                intent.putExtras(intent_template);
                break;

            case STATE_JOB_PAUSED:
                intent_template = super.activeJobFlow();
                intent = new Intent(super.context, JobPausedActivity.class);
                intent.putExtras(intent_template);
                break;

            default:
                // If the state is not understood, tell the user and show the
                // buttons to retry/change server address
                Log.e(TAG, "State could not be parsed: " + state);
                throw new JSONException("Could not parse state");
        }

        return intent;
    }
}
