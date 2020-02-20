package uk.co.digitme.machinemonitoring.Pneumatrol;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import uk.co.digitme.machinemonitoring.LoggedInActivity;
import uk.co.digitme.machinemonitoring.R;


/**
 * This activity requests a job number from the user and returns it to the last screen
 *
 * This activity does not send or request anything to/from the server
 */

public class JobNumberActivity extends LoggedInActivity {

    EditText mJobNumberEditText;
    Button mEnterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set to fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Stop the screen timeout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.pneumatrol_activity_job_number);

        getSupportActionBar().setTitle("Start a Job");

        mJobNumberEditText = findViewById(R.id.job_number_edittext);
        mEnterButton = findViewById(R.id.enter_button);

        // Focus on the box and open the keyboard when the activity starts
        mJobNumberEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        // Clicking the Enter button ends the activity and returns the job number in the box
        mEnterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = getIntent();
                intent.putExtra("jobNumber", mJobNumberEditText.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
            }
        });


    }
}
