package uk.co.digitme.machinemonitoring;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.Objects;

public class SettingInProgress extends LoggedInActivity {

    public static final int SETTING_END_DATA_REQUEST_CODE = 9003;
    String jobNumber;

    Button mEndButton;

    DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_in_progress);

        dbHelper = new DbHelper(getApplicationContext());

        // Change the colour of the background. The colour is sent by the server
        String colour = getIntent().getStringExtra("colour");
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setBackgroundColor(Color.parseColor(colour));

        // Set the action bar to read the job number
        jobNumber = getIntent().getStringExtra("jobNumber");
        Log.v(TAG, "Job number: " + jobNumber);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Job in progress: " + jobNumber);

        mEndButton = findViewById(R.id.end_job_button);

        mEndButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                endSetting();
            }
        });
    }

    private void endSetting(){
        Intent endSettingInfoIntent = new Intent(getApplicationContext(), EndJobActivity.class);
        endSettingInfoIntent.putExtra("requestCode", SETTING_END_DATA_REQUEST_CODE);
        startActivityForResult(endSettingInfoIntent, SETTING_END_DATA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // When the job end information returns, send the info to the server to end the job
        if (requestCode == SETTING_END_DATA_REQUEST_CODE) {
            try {
                Bundle bundle = data.getExtras();
                int scrap = bundle.getInt("quantity", 0);
                RequestQueue queue = Volley.newRequestQueue(this);
                String url = "http://" + dbHelper.getServerAddress() + "/androidendjob";
                JSONObject jsonPostBody = new JSONObject();
                jsonPostBody.put("quantity", scrap);
                jsonPostBody.put("setting", true);

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                        url,
                        jsonPostBody,
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
                }
            }
        }
    }
}
