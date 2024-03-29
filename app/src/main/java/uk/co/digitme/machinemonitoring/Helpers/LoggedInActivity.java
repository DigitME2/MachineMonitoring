package uk.co.digitme.machinemonitoring.Helpers;

import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import uk.co.digitme.machinemonitoring.R;


/**
 * This is a base class for any activity that needs to show a "log off" button in the action barh
 */

public abstract class LoggedInActivity extends AppCompatActivity {

    public final String TAG = "BaseActivity";
    final public int RESULT_LOGOUT = 2;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.logout) {
            // To close the parent activity
            setResult(RESULT_LOGOUT);
            try {
                DbHelper dbHelper = new DbHelper(getApplicationContext());
                RequestQueue queue = Volley.newRequestQueue(this);
                String url = dbHelper.getServerAddress() + "/android-logout";

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("device_uuid", dbHelper.getDeviceUuid());

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                        url,
                        jsonBody,
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
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // This activity will close if logout is clicked on one of its child activities
        if (resultCode == RESULT_LOGOUT) {
            finish();
        }
    }
}
