package uk.co.digitme.machinemonitoring.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import static uk.co.digitme.machinemonitoring.MainActivity.DEFAULT_URL;

public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = "Database Helper";

    private static final String DATABASE_NAME = "Prod.db";
    private static final int DATABASE_VERSION = 1;

    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_SERVER_ADDRESS = "SERVER_ADDRESS";

    public DbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS SETTINGS (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SERVER_ADDRESS + " TEXT NOT NULL); ";

        db.execSQL(SQL_CREATE_TABLE);

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, 1);
        cv.put(COLUMN_SERVER_ADDRESS, DEFAULT_URL);

        db.replace("SETTINGS", null ,cv);
        Log.d(TAG, "Saving server address: " + DEFAULT_URL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        String address = getServerAddress();

    }

    public String getServerAddress(){
        SQLiteDatabase db = getReadableDatabase();

        final String SQL_GET_ADDRESS = "SELECT SERVER_ADDRESS FROM SETTINGS WHERE ID=1 Limit 1";
        //Cursor cursor = db.rawQuery(SQL_GET_ADDRESS, null);
        //Cursor cursor = db.query("SETTINGS", new String[] {"SERVER_ADDRESS"},
        //        "ID=1", null, null, null, null);
        Cursor cursor = db.rawQuery("SELECT * FROM SETTINGS",null);
        if(cursor.moveToFirst()) {
            String address;
            try {
                address = cursor.getString(cursor.getColumnIndex(COLUMN_SERVER_ADDRESS));
                cursor.close();
            } catch (CursorIndexOutOfBoundsException e) {
                address = "";
            }
            return address;
        }
        return "";
    }

    public void saveServerAddress(String address){
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, 1);
        cv.put(COLUMN_SERVER_ADDRESS, address);

        db.replace("SETTINGS", null ,cv);
        Log.d(TAG, "Saving server address: " + address);
    }
}
