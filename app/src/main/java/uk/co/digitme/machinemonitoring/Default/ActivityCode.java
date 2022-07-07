package uk.co.digitme.machinemonitoring.Default;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class ActivityCode {
    public int activityCodeId;
    public String description;
    public String colour;

    public ActivityCode(int activityCodeId, String description, String colour){
        this.activityCodeId  = activityCodeId;
        this.description = description;
        this.colour = colour;
    }

    public ActivityCode(JSONObject jsonActivityCode) throws JSONException {
        this.activityCodeId  = jsonActivityCode.getInt("activity_code_id");
        this.description = jsonActivityCode.getString("description");
        this.colour = jsonActivityCode.getString("colour");
    }

    @NonNull
    @Override
    public String toString(){
        return description;
    }
}
