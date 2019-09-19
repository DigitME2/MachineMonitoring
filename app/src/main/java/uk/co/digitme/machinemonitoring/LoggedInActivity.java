package uk.co.digitme.machinemonitoring;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;


/**
 * This is a base class for any activity that needs to show a "log off" button in the action barh
 */

public abstract class LoggedInActivity extends AppCompatActivity {

    final String TAG = "BaseActivity";
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
            // In case there is another activity expecting a result
            setResult(RESULT_LOGOUT);
            try {
                DbHelper dbHelper = new DbHelper(getApplicationContext());
                RequestQueue queue = Volley.newRequestQueue(this);
                String url = "http://" + dbHelper.getServerAddress() + "/androidlogout";

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                        url,
                        null,
                        new EndActivityResponseListener(this),
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.v("ErrorListener", String.valueOf(error));
                                Toast.makeText(getApplicationContext(), String.valueOf(error), Toast.LENGTH_LONG).show();
                            }
                        });

                queue.add(jsonObjectRequest);
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e(TAG, e.getMessage());
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
