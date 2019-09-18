package uk.co.digitme.machinemonitoring;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A simple response listener to use for JSON requests
 * Checks the response for "success", and ends the activity if true. Shows errors as a toast
 */
public class EndActivityResponseListener implements Response.Listener<JSONObject> {

    private Activity mActivity;

    /**
     * @param activity The activity to close when a successful response is received
     */
    EndActivityResponseListener(Activity activity){
        this.mActivity = activity;
    }

    @Override
    public void onResponse(JSONObject response) {
        try {
            // Finish the activity if success is true
            if (response.getBoolean("success")) {
                mActivity.finish();
            } else {
                // If success is false, print the reason to the user
                Toast.makeText(mActivity.getApplicationContext(),
                        response.getString("reason"),
                        Toast.LENGTH_SHORT).show();
                mActivity.finish();
            }
        } catch (JSONException e) {
            Log.v("ServerResponseListener", e.toString());
            Log.v("ServerResponseListener", "Failed parsing server response: " + response.toString());
            mActivity.finish();
        }
    }
}
