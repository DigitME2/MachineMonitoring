package uk.co.digitme.machinemonitoring;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import uk.co.digitme.machinemonitoring.Helpers.CustomNumpadView;
import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.Helpers.EndActivityResponseListener;
import uk.co.digitme.machinemonitoring.Helpers.LoggedInActivity;
import uk.co.digitme.machinemonitoring.Helpers.NextKeyListener;
import uk.co.digitme.machinemonitoring.Helpers.OnOneOffClickListener;


/**
 * This activity requests information from the user, and then POSTs the data to a request using
 * Volley. The data requested, and the post address are supplied via an intent
 */
// TODO dont save job numbers as ints cause it will remove the leading zero
public class DataEntryActivity extends LoggedInActivity {

    private final String TAG = "JobInfoActivity";

    DbHelper dbHelper;
    EditText[] editTexts;
    LinearLayout parentLayout;
    Button sendButton;
    JSONObject requestedData;
    String[] requestedDataKeys;
    String[] requestedDataTitles;
    JSONObject requestedDataAutoFill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set to full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dbHelper = new DbHelper(getApplicationContext());
        setContentView(R.layout.numerical_data_input_form);

        // Get the data that has been requested to be collected from the user
        String requestedDataS = getIntent().getStringExtra("requestedData");
        if (requestedDataS == null) {
            Log.e(TAG, "No requested data supplied to DataEntryActivity");
            finish();
            return;
        }
        try {
            requestedData = new JSONObject(requestedDataS);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
        requestedDataTitles = new String[requestedData.length()];
        requestedDataKeys = new String[requestedData.length()];

        // Get any autofill data sent by the server
        try {
            String jsonString = getIntent().getStringExtra("requestedDataAutofill");
            if (jsonString != null) {
                requestedDataAutoFill = new JSONObject(jsonString);
            }
            else {
                requestedDataAutoFill = new JSONObject();  //Initialise empty object
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }


        // Iterate through the JSON object and add the data to arrays
        Iterator<String> iterator = requestedData.keys();
        for (int i = 0; iterator.hasNext(); i++) {
            String key = iterator.next();
            requestedDataKeys[i] = key;
            try {
                requestedDataTitles[i] = requestedData.get(key).toString();
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }

        // For each bit of requested data, create a linear layout with a text view and edittext
        parentLayout = findViewById(R.id.data_entry_parent);
        ViewGroup viewGroup = parentLayout;
        editTexts = new EditText[requestedData.length()];
        boolean numericalInput = getIntent().getBooleanExtra("numericalInput", false);
        for (int i = 0; i < requestedData.length(); i++) {
            LayoutInflater inflater = LayoutInflater.from(this);
            LinearLayout newRow = (LinearLayout) inflater.inflate(R.layout.data_input_item, viewGroup, false);
            parentLayout.addView(newRow);
            editTexts[i] = newRow.findViewById(R.id.data_item_edit_text);
            TextView tv = newRow.findViewById(R.id.data_item_title_textview);
            tv.setText(requestedDataTitles[i]);
            if (numericalInput) {
                editTexts[i].setShowSoftInputOnFocus(false);
            }
            // Auto fill the edit text if a value has been given
            if (requestedDataAutoFill.has(requestedDataKeys[i])){
                try {
                    editTexts[i].setText(requestedDataAutoFill.getString(requestedDataKeys[i]));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.toString());
                }
            }
        }

        // Create the send button
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout sendLine = (LinearLayout) inflater.inflate(R.layout.data_input_button, parentLayout);
        sendButton = sendLine.findViewById(R.id.send_button);
        sendButton.setText(getIntent().getStringExtra("sendButtonText"));

        // Set the on click listener for the "next" button on the keyboard
        for (int i = 0; i < requestedData.length(); i++) {
            // Don't do it for the last edittext
            if (i != editTexts.length - 1) {
                editTexts[i].setOnKeyListener(new NextKeyListener(editTexts[i + 1]));
            }
            // Set the key listener for the last edittext, so that the next button sends the data
            editTexts[editTexts.length - 1].setOnKeyListener(new NextKeyListener(sendButton));
            // Focus on the top box when activity first opens
            editTexts[0].requestFocus();

            // Clicking the Save button ends the activity and returns the data in the boxex
            sendButton.setOnClickListener(new OnOneOffClickListener() {
                @Override
                public void onSingleClick(View view) {
                    send();
                }
            });

            //Set up the custom keyboard, if requested
            if (numericalInput) {
                CustomNumpadView cnv = findViewById(R.id.keyboard_view);
                cnv.setActionListenerActivity(this);
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            }
        }
    }


    private void send() {

        JSONObject resultJson = new JSONObject();

        for (int i = 0; i < requestedData.length(); i++) {

            if (TextUtils.isEmpty(editTexts[i].getText().toString())) {
                Toast.makeText(getApplicationContext(), "All fields are not filled in", Toast.LENGTH_SHORT).show();
                // Reset the key listener for the last EditText.
                // It's disabled after "send" is clicked, to stop double presses
                editTexts[editTexts.length - 1].setOnKeyListener(new NextKeyListener(sendButton));
                return;
            }
            // Add the data to the JSON
            try {
                resultJson.put(requestedDataKeys[i], editTexts[i].getText());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            // The URL is sent by the requesting activity
            String url = "http://" + dbHelper.getServerAddress() + getIntent().getStringExtra("url");

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    resultJson,
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
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

}


