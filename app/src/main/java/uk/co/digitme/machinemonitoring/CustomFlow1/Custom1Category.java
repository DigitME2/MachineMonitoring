package uk.co.digitme.machinemonitoring.CustomFlow1;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class Custom1Category {
    public String categoryId;
    public String categoryName;

    public Custom1Category(JSONObject jsonCategory) throws JSONException {
        this.categoryId  = jsonCategory.getString("category");
        this.categoryName = jsonCategory.getString("category_name");
    }

    @NonNull
    @Override
    public String toString() {
        return categoryName;
    }

    public String getCategory() {
        return this.categoryId;
    }
}
