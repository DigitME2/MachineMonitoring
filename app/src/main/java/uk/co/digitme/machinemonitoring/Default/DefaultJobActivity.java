package uk.co.digitme.machinemonitoring.Default;

import android.os.Bundle;

import uk.co.digitme.machinemonitoring.Helpers.LoggedInActivity;
import uk.co.digitme.machinemonitoring.R;


/**
 * The activity shown when the server reports this device's job is in progress
 */
public class DefaultJobActivity extends JobActivityBase {

    final String TAG = "DefaultJobActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_job_in_progress);
        super.onCreate(savedInstanceState);
        }

}
