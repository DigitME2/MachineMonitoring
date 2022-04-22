package uk.co.digitme.machinemonitoring.RunningTotal;

import android.content.Context;

import org.json.JSONObject;

import uk.co.digitme.machinemonitoring.Default.Workflow;

public class RunningTotalWorkflow extends Workflow {
    public RunningTotalWorkflow(Context context, JSONObject serverResponse) {
        super(context, serverResponse);
    }
}
