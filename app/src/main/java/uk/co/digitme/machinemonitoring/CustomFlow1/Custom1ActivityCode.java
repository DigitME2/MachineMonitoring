package uk.co.digitme.machinemonitoring.CustomFlow1;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.digitme.machinemonitoring.Default.ActivityCode;

public class Custom1ActivityCode  extends ActivityCode{
    public String category;
    public Custom1ActivityCode(JSONObject jsonActivityCode) throws JSONException {
        super(jsonActivityCode);
        this.category = jsonActivityCode.getString("category");
    }
    @NonNull
    @Override
    public String toString() {
        return description;
    }
}
