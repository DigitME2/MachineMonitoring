package uk.co.digitme.machinemonitoring;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import androidx.appcompat.app.ActionBar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

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
public class DataEntryActivity extends LoggedInActivity {

    private final String TAG = "DataEntryActivity";
    public final static String TITLE_KEY = "title";
    public final static String TYPE_KEY = "type";
    public final static String AUTOFILL_KEY = "autofill";
    public final static String VALIDATION_KEY = "validation";
    public final static String WARNING_KEY = "warning";

    DbHelper dbHelper;
    TextView instructionsTV;
    EditText[] editTexts;
    TextView[] validationTVs;
    LinearLayout parentLayout;
    Button sendButton;
    JSONObject requestedData;
    String[] requestedDataKeys;
    JSONObject[] requestedDataItems;
    JSONObject requestedDataAutoFill;

    // To display the current time in the edittext as default
    BroadcastReceiver _broadcastReceiver;
    private final SimpleDateFormat _sdfWatchTime = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        dbHelper = new DbHelper(getApplicationContext());
        setContentView(R.layout.data_entry_activity);

        // Set up the action bar
        String actionBarTitle = getIntent().getStringExtra("actionBarTitle");
        String actionBarSubtitle = getIntent().getStringExtra("actionBarSubtitle");
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            if (actionBarTitle != null) {
                ab.setTitle(actionBarTitle);
            } else {
                ab.hide();
            }
            if (actionBarSubtitle != null) {
                ab.setSubtitle(actionBarSubtitle);
            }
        }
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
        requestedDataItems = new JSONObject[requestedData.length()];
        requestedDataKeys = new String[requestedData.length()];

        // Get any autofill data sent by the server
        try {
            String jsonString = getIntent().getStringExtra("requestedDataAutofill");
            if (jsonString != null) {
                requestedDataAutoFill = new JSONObject(jsonString);
            } else {
                requestedDataAutoFill = new JSONObject();  //Initialise empty object
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }

        // If given instructions, show them at the top in a textview, otherwise hide the textview
        String instructions = getIntent().getStringExtra("instructionText");
        instructionsTV = findViewById(R.id.data_entry_instructions);
        if (instructions == null || instructions.length() < 1) {
            instructionsTV.setVisibility(View.GONE);
        } else {
            instructionsTV.setVisibility(View.VISIBLE);
            instructionsTV.setText(instructions);
        }

        // Iterate through the JSON object and add the data to arrays
        Iterator<String> iterator = requestedData.keys();
        for (int i = 0; iterator.hasNext(); i++) {
            String key = iterator.next();
            requestedDataKeys[i] = key;
            try {
                requestedDataItems[i] = (JSONObject) requestedData.get(key);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }

        // For each bit of requested data, create a linear layout with a text view and edittext
        parentLayout = findViewById(R.id.data_entry_parent);
        ViewGroup viewGroup = parentLayout;
        editTexts = new EditText[requestedData.length()];
        validationTVs = new TextView[requestedData.length()];
        boolean allNumericalInput = true;
        for (int i = 0; i < requestedData.length(); i++) {
            String title;
            String inputType;
            String autofill;
            LayoutInflater inflater = LayoutInflater.from(this);
            LinearLayout newRow = (LinearLayout) inflater.inflate(R.layout.data_input_item, viewGroup, false);
            parentLayout.addView(newRow);
            final EditText editText = newRow.findViewById(R.id.data_item_edit_text);
            // Need to set this to fix a bug causing all edit texts to update with the time
            editText.setSaveEnabled(false);
            TextView tv = newRow.findViewById(R.id.data_item_title_textview);
            try {
                title = requestedDataItems[i].getString(TITLE_KEY);
                autofill = requestedDataItems[i].getString(AUTOFILL_KEY);
                inputType = requestedDataItems[i].getString(TYPE_KEY);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
                continue;
            }
            tv.setText(title);

            LinearLayout validationRow = (LinearLayout) inflater.inflate(R.layout.data_validation_item, viewGroup, false);
            parentLayout.addView(validationRow);
            TextView validationTextView = validationRow.findViewById(R.id.validation_text);
            validationTVs[i] = validationTextView;
            validationTVs[i].setVisibility(View.GONE);
            switch (inputType) {
                case "text":
                    if (!autofill.equals("")) {
                        editText.setText(autofill);
                    }
                    allNumericalInput = false;
                    break;

                case "number":
                    editText.setText(autofill);
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    break;

                case "time":
                    editText.setShowSoftInputOnFocus(false);
                    editText.setFocusable(false);
                    if (autofill.equals("current")) {
                        // Set a broadcast receiver to update the time to the current time
                        _broadcastReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0)
                                    editText.setText(_sdfWatchTime.format(new Date()));
                            }
                        };
                        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
                    }
                    editText.setOnClickListener(view -> {
                        try {
                            // Remove the broadcast receiver to stop the time updates
                            unregisterReceiver(_broadcastReceiver);
                        } catch (IllegalArgumentException e) {
                            // If the receiver is already registered
                        }
                        final Calendar c = Calendar.getInstance();
                        int hour = c.get(Calendar.HOUR_OF_DAY);
                        int minute = c.get(Calendar.MINUTE);
                        TimePickerDialog mTimePicker = new TimePickerDialog(DataEntryActivity.this, (timePicker, hourOfDay, minuteOfDay) -> {
                            String hourString = Integer.toString(hourOfDay);
                            String minuteString = Integer.toString(minuteOfDay);
                            if (hourOfDay < 10) {
                                hourString = "0" + hourOfDay;
                            }
                            if (minuteOfDay < 10) {
                                minuteString = "0" + minuteOfDay;
                            }
                            String timeString = hourString + ":" + minuteString;
                            editText.setText(timeString);
                        }, hour, minute, true);

                        mTimePicker.show();
                    });

                    break;
            }
            editTexts[i] = editText;
        }

        // Create the send button
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout sendLine = (LinearLayout) inflater.inflate(R.layout.data_input_button, parentLayout);
        sendButton = sendLine.findViewById(R.id.send_button);
        sendButton.setText(getIntent().getStringExtra("sendButtonText"));

        // Focus on the top box when activity first opens
        editTexts[0].requestFocus();
        sendButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                send(false);
            }
        });
        //Set up the custom keyboard, if requested
        if (allNumericalInput) {
            CustomNumpadView cnv = findViewById(R.id.keyboard_view);
            cnv.setActionListenerActivity(this);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            for (int i = 0; i < requestedData.length(); i++) {
                editTexts[i].setShowSoftInputOnFocus(false);
            }
        }

        // Set up data validation text (show extra info when data is entered by the user)
        for (int i = 0; i < requestedData.length(); i++) {
            try {
                if (requestedDataItems[i].has(WARNING_KEY)) {
                    validationTVs[i].setText(requestedDataItems[i].getString(WARNING_KEY));
                    validationTVs[i].setVisibility(View.VISIBLE);
                } else if (requestedDataItems[i].has(VALIDATION_KEY)) {
                    JSONObject validation = new JSONObject(requestedDataItems[i].getString(VALIDATION_KEY));
                    editTexts[i].addTextChangedListener(new DataValidationTextWatcher(validationTVs[i], validation));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onResume() {
        // Check the requestedData to see if any edit texts need the current time updating
        for (int i = 0; i < requestedData.length(); i++) {
            String inputType;
            String autofill;
            try {
                autofill = requestedDataItems[i].getString(AUTOFILL_KEY);
                inputType = requestedDataItems[i].getString(TYPE_KEY);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
                continue;
            }
            if (inputType.equals("time") && autofill.equals("current")) {
                editTexts[i].setText(_sdfWatchTime.format(new Date()));
            }
        }
        super.onResume();
    }


    private void send(Boolean ignoreValidation) {

        JSONObject validation = null;
        JSONObject resultJson = new JSONObject();

        for (int i = 0; i < requestedData.length(); i++) {

            // Validate the data if necessary
            if (!ignoreValidation && requestedDataItems[i].has(VALIDATION_KEY)) {
                try {
                    // Get the list of values that the entered amount is allowed to be
                    validation = new JSONObject(requestedDataItems[i].getString(VALIDATION_KEY));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Create a dialog box if the edit text value is not in the list of valid entries
                if (validation != null && !validation.toString().contains(editTexts[i].getText())) {
                    DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            // Repeat without data validation
                            send(true);
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(DataEntryActivity.this);
                    String dialogText;
                    try {
                        dialogText = "WARNING: " + requestedDataItems[i].getString(TITLE_KEY) + " did not pass data validation. Continue anyway?";
                    } catch (JSONException e) {
                        dialogText = "Data looks incorrect. Send anyway?";
                        e.printStackTrace();
                    }
                    builder.setMessage(dialogText).setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                    return;
                }
            }

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
            String url = dbHelper.getServerAddress() + getIntent().getStringExtra("url");
            resultJson.put("device_uuid", dbHelper.getDeviceUuid());
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    resultJson,
                    new EndActivityResponseListener(this),
                    error -> {
                        Log.v("ErrorListener", String.valueOf(error));
                        Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                        finish();
                    });
            Log.d(TAG, "POSTing to " + url);
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

    private static class DataValidationTextWatcher implements TextWatcher {
        TextView validationTV;
        JSONObject validationDict;

        public DataValidationTextWatcher(TextView validationTV, JSONObject validationDict) {
            this.validationTV = validationTV;
            this.validationDict = validationDict;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            try {
                String validationText = validationDict.getString(charSequence.toString());
                this.validationTV.setText(validationText);
                this.validationTV.setVisibility(View.VISIBLE);
            } catch (JSONException e) {
                this.validationTV.setText("");
                this.validationTV.setVisibility(View.GONE);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }
}


