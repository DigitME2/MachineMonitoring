package uk.co.digitme.machinemonitoring;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.digitme.machinemonitoring.Default.Workflow;
import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;
import uk.co.digitme.machinemonitoring.Pausable.PausableWorkflow;
import uk.co.digitme.machinemonitoring.RunningTotal.RunningTotalWorkflow;

/**
 * The app goes to this screen every time it is opens. This activity contacts the server to find out
 * what state this app should be in (eg no user, job active). This activity then starts the activity
 * <p>
 * If the activity cannot connect to the server, it shows options to retry and change the server ip
 */


public class MainActivity extends AppCompatActivity {

    public static final String DEFAULT_URL = "http://192.168.0.100";

    public static final int REQUEST_LOGIN = 9000;
    public static final int REQUEST_START_JOB = 4000;
    public static final int REQUEST_END_JOB = 4001;
    public static final int RESULT_NO_JOB = 8001;

    public static final String TAG = "MainActivity";

    TextView mStatusText;
    Button mRetryButton;
    Button mSetAddressButton;

    DbHelper dbHelper;
    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dbHelper = new DbHelper(getApplicationContext());
        prefs = getSharedPreferences("uk.samban.machinemonitoring", MODE_PRIVATE);

        setContentView(R.layout.activity_main);
        //TODO Fix the error that shows up when the server can't be found
        getSupportActionBar().setTitle("");

        // Status text and retry/change server buttons.
        // These will become visible after failing to connect.
        mStatusText = findViewById(R.id.main_activity_status);
        mRetryButton = findViewById(R.id.retry_button);

        // The retry button attempts to contact the server again.
        mRetryButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View v) {
                checkState();
            }
        });
        // The "set address" button opens a new activity allowing the user to change the server ip
        mSetAddressButton = findViewById(R.id.set_address_button);
        mSetAddressButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // On the first run, show the "set address" button immediately
        if (prefs.getBoolean("firstrun", true)) {
            mStatusText.setVisibility(View.INVISIBLE);
            mRetryButton.setVisibility(View.INVISIBLE);
            mSetAddressButton.setVisibility(View.VISIBLE);
            prefs.edit().putBoolean("firstrun", false).apply();
        } else {
            // Hide the buttons by default. This stops them showing during transitions between activities
            mStatusText.setVisibility(View.INVISIBLE);
            mRetryButton.setVisibility(View.INVISIBLE);
            mSetAddressButton.setVisibility(View.INVISIBLE);
            // When arriving at this page, immediately contact the server to see which screen the app
            // should be on, and start that activity
            checkState();
        }
    }

    private void showError(String errorText) {
        mStatusText.setText(errorText);
        mStatusText.setVisibility(View.VISIBLE);
        mRetryButton.setVisibility(View.VISIBLE);
        mSetAddressButton.setVisibility(View.VISIBLE);
    }

    /**
     * Contacts the server to get the current state of this device e.g. active job, no user
     * Also launches the corresponding activity
     */
    private void checkState() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = dbHelper.getServerAddress() + "/check-state";
        @SuppressLint("SetTextI18n") JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        checkStateResponseHandler(response);
                    } catch (JSONException e) {
                        showError("Error parsing server response");
                    }
                }, error -> {
            // Show a different error message depending on the error
            if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                showError("Could not connect to network");
            } else if (error instanceof ServerError) {
                showError("Could not connect to server");
            }
            Log.v("ErrorListener", String.valueOf(error));
            Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
        });
        queue.add(jsonObjectRequest);
    }


    /**
     * When getting a response from the server from /check-state, create a workflow object and
     * then start a new activity according to the workflow
     */
    private void checkStateResponseHandler(JSONObject response) throws JSONException {
        {
            String workflowType;
            Workflow workflow;
            workflowType = response.getString("workflow_type");

            switch (workflowType) {
                case "default":
                    workflow = new Workflow(getApplicationContext(), response);
                    break;

                case "pausable":
                    workflow = new PausableWorkflow(getApplicationContext(), response);
                    break;

                case "running_total":
                    workflow = new RunningTotalWorkflow(getApplicationContext(), response);
                    break;

                default:
                    showError(String.format("App version not compatible with workflow type %s", workflowType));
                    return;
            }
            Intent nextIntent;
            nextIntent = workflow.getIntent();
            startActivity(nextIntent);
        }
    }
}
