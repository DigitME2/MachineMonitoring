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

    EditText mPlannedSetTime;
    EditText mPlannedRunTimeEditText;
    EditText mPlannedQuantityEditText;
    EditText mPlannedCycleTime;

    Button mStartButton;
    Button mStartSettingButton;

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
        mPlannedSetTime = findViewById(R.id.data_entry_4);
        mPlannedRunTimeEditText = findViewById(R.id.data_entry_1);
        mPlannedQuantityEditText = findViewById(R.id.data_entry_2);
        mPlannedCycleTime = findViewById(R.id.data_entry_3);
        mStartButton = findViewById(R.id.start_button);
        mStartSettingButton = findViewById(R.id.start_setting_button);

        //Set up the custom keyboard
        CustomNumpadView cnv = findViewById(R.id.keyboard_view);
        cnv.setActionListenerActivity(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Focus on the top box when activity first opens
        mPlannedSetTime.requestFocus();
        mPlannedSetTime.setOnKeyListener(new View.OnKeyListener(){
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode==66){
                    // Prevent this from being clicked again
                    mPlannedSetTime.setOnKeyListener(null);
                    // Send the post request to start setting
                    startSetting();
                }
                return false;
            }
        });

        //Set the behaviour for the "Next" button to tab through edittexts
        mPlannedRunTimeEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode==66){
                    mPlannedQuantityEditText.requestFocus();
                }
                return false;
            }
        });
        mPlannedQuantityEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode==66){
                    mPlannedCycleTime.requestFocus();
                }
                return false;
            }
        });
        mPlannedCycleTime.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode==66){
                    // Prevent this from being clicked again
                    mPlannedCycleTime.setOnKeyListener(null);
                    // Send the post request to start the job
                    startJob();

                }
                return false;
            }
        });

        // Prevent the default keyboard from opening
        mPlannedRunTimeEditText.setShowSoftInputOnFocus(false);
        mPlannedQuantityEditText.setShowSoftInputOnFocus(false);
        mPlannedCycleTime.setShowSoftInputOnFocus(false);

        // The start button sends a request to the server to start the job, and finishes this activity
        mStartButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                startJob();
            }
        });

        mStartSettingButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                startSetting();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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
        if (TextUtils.isEmpty(mPlannedRunTimeEditText.getText().toString()) ||
                TextUtils.isEmpty(mPlannedQuantityEditText.getText().toString()) ||
                TextUtils.isEmpty(mPlannedCycleTime.getText().toString())) {
            Toast.makeText(getApplicationContext(), "All fields are not filled in", Toast.LENGTH_SHORT).show();
            // Reset the key listener on the last EditText.
            // It's disabled after being clicked to stop double presses
            mPlannedCycleTime.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == 66) {
                        // Prevent this from being clicked again
                        mPlannedCycleTime.setOnKeyListener(null);
                        // Send the post request to start the job
                        startJob();
                    }
                    return false;
                }
            });
            return;
        }


        // Send the login request to the server. End this activity if successful
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + dbHelper.getServerAddress() + "/androidstartjob";
            JSONObject jsonRequestBody = new JSONObject();
            int plannedRunTime = Integer.parseInt(mPlannedRunTimeEditText.getText().toString());
            int plannedQuantity = Integer.parseInt(mPlannedQuantityEditText.getText().toString());
            int plannedCycleTime = Integer.parseInt(mPlannedCycleTime.getText().toString());

            jsonRequestBody.put("setting", false);
            jsonRequestBody.put("wo_number", mJobNumber);
            jsonRequestBody.put("planned_run_time", plannedRunTime);
            jsonRequestBody.put("planned_quantity", plannedQuantity);
            jsonRequestBody.put("planned_cycle_time", plannedCycleTime);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonRequestBody,
                    new EndActivityResponseListener(this),
                    new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v("ErrorListener", String.valueOf(error));
                    Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                    finish();
                }
            });

            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(getApplicationContext(), String.valueOf(e.getMessage()), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    private void startSetting() {
        //Check all the fields are filled in
        if (TextUtils.isEmpty(mPlannedSetTime.getText().toString())) {
            Toast.makeText(getApplicationContext(), "No planned set time given", Toast.LENGTH_SHORT).show();
            // Reset the key listener on the last EditText.
            // It's disabled after being clicked to stop double presses
            mPlannedSetTime.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == 66) {
                        // Prevent this from being clicked again
                        mPlannedSetTime.setOnKeyListener(null);
                        // Send the post request to start the setting
                        startJob();
                    }
                    return false;
                }
            });
            return;
        }


        // Send the login request to the server. End this activity if successful
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + dbHelper.getServerAddress() + "/androidstartjob";
            JSONObject jsonRequestBody = new JSONObject();
            int plannedSetTime = Integer.parseInt(mPlannedSetTime.getText().toString());
            jsonRequestBody.put("wo_number", mJobNumber);
            jsonRequestBody.put("setting", true);
            jsonRequestBody.put("planned_set_time", plannedSetTime);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonRequestBody,
                    new EndActivityResponseListener(this),
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.v("ErrorListener", String.valueOf(error));
                            Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });

            queue.add(jsonObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(getApplicationContext(), String.valueOf(e.getMessage()), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

}


