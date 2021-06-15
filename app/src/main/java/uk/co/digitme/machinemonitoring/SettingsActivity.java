package uk.co.digitme.machinemonitoring;

import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import uk.co.digitme.machinemonitoring.Helpers.DbHelper;
import uk.co.digitme.machinemonitoring.R;


/**
 * An activity to change the address of the server being contacted.
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String LOCK_KEY = "lock";
    static final String TAG = "Settings";

    Button saveButton;
    Switch lockTaskSwitch;
    EditText serverAddressEditText;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    DevicePolicyManager devicePolicyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final DbHelper dbHelper = new DbHelper(getApplicationContext());

        lockTaskSwitch = findViewById(R.id.lock_task_switch);
        serverAddressEditText = findViewById(R.id.server_address_edittext);
        serverAddressEditText.setText(dbHelper.getServerAddress());

        preferences = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        editor = preferences.edit();

        //Code to change the admin key (admin key is now hardcoded)
        /*adminNumberEditText = (EditText) findViewById(R.id.admin_clock_number);
        adminNumberEditText.setText(preferences.getString(ADMIN_CLOCK_NUMBER_KEY,"1992"));*/



        //If the app is not a device owner, disable the lock task switch
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!devicePolicyManager.isDeviceOwnerApp(getApplicationContext().getPackageName())) {
            lockTaskSwitch.setVisibility(View.GONE);
        }

        //Set up the lock task switch
        lockTaskSwitch.setChecked(preferences.getBoolean(LOCK_KEY, false));
        lockTaskSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean(LOCK_KEY, isChecked);
                editor.apply();
            }
        });


        //Activating task lock sometimes takes a few seconds, so show a progress bar
        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Save the new address and then close
                dbHelper.saveServerAddress(serverAddressEditText.getText().toString());

                finish();
            }
        });


    }
}