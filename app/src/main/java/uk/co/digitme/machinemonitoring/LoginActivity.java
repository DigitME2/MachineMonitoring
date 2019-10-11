package uk.co.digitme.machinemonitoring;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

/**
 * The login screen to show when a user is not logged in.
 */

public class LoginActivity extends AppCompatActivity {

    private final String TAG = "LoginActivity";

    TextView mMachineNameText;
    EditText userIdEditText;
    EditText pinCodeEditText;
    Button mSignInButton;

    DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DbHelper(getApplicationContext());
        setContentView(R.layout.activity_login);
        String IP = getIntent().getStringExtra("IP");
        getSupportActionBar().setTitle(IP + " - Log in");

        //Set up the custom keyboard
        CustomNumpadView cnv = findViewById(R.id.keyboard_view);
        cnv.setActionListenerActivity(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mMachineNameText = findViewById(R.id.machine_name);
        userIdEditText = findViewById(R.id.login_id);
        pinCodeEditText = findViewById(R.id.login_password);
        mSignInButton = findViewById(R.id.sign_in_button);

        // Set the machine text
        mMachineNameText.setText(getIntent().getStringExtra("machineText"));

        // Focus on the user ID on startup
        userIdEditText.requestFocus();

        //Set the behaviour for the "Next" button to tab through edittexts
        userIdEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == 66) {
                    pinCodeEditText.requestFocus();
                }
                return false;
            }
        });
        pinCodeEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == 66) {
                    // Prevent this from being clicked again
                    pinCodeEditText.setOnKeyListener(null);
                    // Send the post request to login the user
                    logIn();
                }
                return false;
            }
        });

        // Prevent the default keyboard from showing
        userIdEditText.setShowSoftInputOnFocus(false);
        pinCodeEditText.setShowSoftInputOnFocus(false);

        // Log in when clicking the login button
        mSignInButton.setOnClickListener(new OnOneOffClickListener() {
            @Override
            public void onSingleClick(View view) {
                logIn();
            }
        });

    }


    private void logIn() {

        final Context context = getApplicationContext();

        //Check all the fields are filled in
        if (
                TextUtils.isEmpty(pinCodeEditText.getText().toString()) ||
                        TextUtils.isEmpty(userIdEditText.getText().toString())) {
            Toast.makeText(context, "All fields are not filled in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send the login request to the server. End this activity if successful
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + dbHelper.getServerAddress() + "/androidlogin";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("user_id", userIdEditText.getText().toString());
            jsonBody.put("password", pinCodeEditText.getText().toString());

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    url,
                    jsonBody,
                    new EndActivityResponseListener(this),
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.v("ErrorListener", String.valueOf(error));
                            Toast.makeText(context, String.valueOf(error), Toast.LENGTH_LONG).show();
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


