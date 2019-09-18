package uk.co.digitme.machinemonitoring;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * This activity requests information from the user about a job before starting it
 */

public class JobInfoActivity extends LoggedInActivity {

    private final String TAG = "JobInfoActivity";

    EditText plannedQuantityEditText;
    EditText plannedCycleTimeEditText;
    EditText plannedCycleQuantityEditText;

    Button mStartButton;

    String mJobNumber;

    DbHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        Intent jobNumberIntent = new Intent(getApplicationContext(), JobNumberActivity.class);

        // Upon opening, immediately open an activity to get the job number
        // The job number isn't entered from this screen because it has a simple numeric keyboard
        startActivityForResult(jobNumberIntent, 9000);

        setContentView(R.layout.activity_job_info);
        plannedQuantityEditText = findViewById(R.id.data_entry_1);
        plannedCycleTimeEditText = findViewById(R.id.data_entry_2);
        plannedCycleQuantityEditText = findViewById(R.id.data_entry_3);
        mStartButton = findViewById(R.id.start_button);

        //Set up the custom keyboard
        CustomNumpadView cnv = findViewById(R.id.keyboard_view);
        cnv.setActionListenerActivity(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Focus on the top box when activity first opens
        plannedQuantityEditText.requestFocus();

        //Set the behaviour for the "Next" button to tab through edittexts
        plannedQuantityEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode==66){
                    plannedCycleTimeEditText.requestFocus();
                }
                return false;
            }
        });
        plannedCycleTimeEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode==66){
                    plannedCycleQuantityEditText.requestFocus();
                }
                return false;
            }
        });
        plannedCycleQuantityEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode==66){
                    // Prevent this from being clicked again
                    plannedCycleQuantityEditText.setOnKeyListener(null);
                    // Send the post request to start the job
                    startJob();
                }
                return false;
            }
        });

        // Prevent the default keyboard from opening
        plannedQuantityEditText.setShowSoftInputOnFocus(false);
        plannedCycleTimeEditText.setShowSoftInputOnFocus(false);
        plannedCycleQuantityEditText.setShowSoftInputOnFocus(false);

        // The start button sends a request to the server to start the job, and finishes this activity
        mStartButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                startJob();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_LOGOUT){
            finish();
        }
        try{
            Bundle bundle = data.getExtras();
            mJobNumber = bundle.getString("jobNumber");
            getSupportActionBar().setTitle("Starting W/O " + mJobNumber);
        }
        catch(NullPointerException npe){
            Log.e(TAG, npe.toString());
        }
    }

    private void startJob() {

        //Check all the fields are filled in
        if (
                TextUtils.isEmpty(plannedQuantityEditText.getText().toString()) ||
                        TextUtils.isEmpty(plannedCycleTimeEditText.getText().toString()) ||
                        TextUtils.isEmpty(plannedCycleQuantityEditText.getText().toString())) {
            Toast.makeText(getApplicationContext(), "All fields are not filled in", Toast.LENGTH_SHORT).show();
            // Reset the key listener on the last edittext.
            // It's disabled after being clicked to stop double presses
            plannedCycleQuantityEditText.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if(keyCode==66){
                        // Prevent this from being clicked again
                        plannedCycleQuantityEditText.setOnKeyListener(null);
                        // Send the post request to start the job
                        startJob();
                    }
                    return false;
                }
            });
            return;
        }

        final long time = System.currentTimeMillis();

        // Send the login request to the server. End this activity if successful
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + dbHelper.getServerAddress() + "/androidstartjob";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("wo_number", mJobNumber);
            jsonBody.put("planned_quantity", plannedQuantityEditText.getText().toString());
            jsonBody.put("planned_cycle_time", plannedCycleTimeEditText.getText().toString());
            jsonBody.put("planned_cycle_quantity", plannedCycleQuantityEditText.getText().toString());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.getBoolean("success")) {
                                    //Intent activeJobIntent = new Intent(getApplicationContext(), JobInProgressActivity.class);
                                    //activeJobIntent.putExtra("jobNumber", mJobNumber);
                                    //startActivity(activeJobIntent);
                                    finish();
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            response.getString("reason"),
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            } catch (JSONException e) {
                                Log.v(TAG, e.toString());
                                Log.v(TAG, "Failed parsing server response: " + response.toString());
                                finish();
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v("ErrorListener", String.valueOf(error));
                    Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                    finish();
                }
            });

            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
                finish();
            }
        }
    }
}


