package uk.co.digitme.machinemonitoring;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import static uk.co.digitme.machinemonitoring.JobInProgressActivity.JOB_END_DATA_REQUEST_CODE;
import static uk.co.digitme.machinemonitoring.SettingInProgress.SETTING_END_DATA_REQUEST_CODE;


/**
 * This activity requests information from the user about a job before starting it
 */

public class EndJobActivity extends LoggedInActivity {

    private final String TAG = "JobInfoActivity";

    Intent intent;

    TextView mQuantityTextView;
    EditText mQuantityEditText;

    Button mSaveButton;

    String mJobNumber;

    DbHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_end_job);
        mQuantityEditText = findViewById(R.id.job_end_data_entry_1);
        mSaveButton = findViewById(R.id.save_button);
        mQuantityTextView = findViewById(R.id.job_end_text_1);

        //Set up the custom keyboard
        CustomNumpadView cnv = findViewById(R.id.keyboard_view);
        cnv.setActionListenerActivity(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        intent = getIntent();
        int requestCode = intent.getIntExtra("requestCode",0);
        if(requestCode==JOB_END_DATA_REQUEST_CODE){
            mQuantityTextView.setText("Actual Quantity");
        }
        else if(requestCode == SETTING_END_DATA_REQUEST_CODE){
            mQuantityTextView.setText("Scrap Quantity");
        }

        // Focus on the top box when activity first opens.setShowSoftInputOnFocus(false);
        mQuantityEditText.requestFocus();

        mQuantityEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode==66){
                    // Prevent this from being clicked again
                    mQuantityEditText.setOnKeyListener(null);
                    // Send the post request to start the job
                    putQuantity();
                    finish();
                }
                return false;
            }
        });

        // Prevent the default keyboard from opening
        mQuantityEditText.setShowSoftInputOnFocus(false);


        // The start button sends a request to the server to start the job, and finishes this activity
        mSaveButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                putQuantity();
                finish();
            }
        });
    }

    private void putQuantity(){
        Intent intent = getIntent();

        if (!TextUtils.isEmpty(mQuantityEditText.getText())){
            int actualQuantity = Integer.parseInt(mQuantityEditText.getText().toString());
            intent.putExtra("quantity", actualQuantity);
            setResult(RESULT_OK, intent);
        }
        else{
            intent.putExtra("quantity", "");
            setResult(RESULT_OK, intent);
        }
    }

}


