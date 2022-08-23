package uk.co.digitme.machinemonitoring.Helpers;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import static uk.co.digitme.machinemonitoring.MainActivity.DEFAULT_URL;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = "Database Helper";

    private static final String DATABASE_NAME = "Prod.db";
    private static final int DATABASE_VERSION = 1;

    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_SERVER_ADDRESS = "SERVER_ADDRESS";
    public static final String COLUMN_DEVICE_UUID = "DEVICE_UUID";

    public DbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS SETTINGS (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SERVER_ADDRESS + " TEXT NOT NULL, " +
                COLUMN_DEVICE_UUID + " TEXT " +
                "); ";

        db.execSQL(SQL_CREATE_TABLE);

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, 1);
        cv.put(COLUMN_SERVER_ADDRESS, DEFAULT_URL);
        cv.put(COLUMN_DEVICE_UUID, UUID.randomUUID().toString());

        db.replace("SETTINGS", null ,cv);
        Log.d(TAG, "Saving server address: " + DEFAULT_URL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
    }

    @SuppressLint("Range")
    public String getServerAddress(){
        SQLiteDatabase db = getReadableDatabase();

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
        else {
            return "";
        }
    }

    public void saveServerAddress(String address){
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, 1);
        cv.put(COLUMN_SERVER_ADDRESS, address);

        db.update("SETTINGS", cv, "ID = ?", new String[] {"1"});
        Log.d(TAG, "Saving server address: " + address);
    }

    @SuppressLint("Range")
    public URI getServerURI() throws URISyntaxException {
        URI uri;
        String address = this.getServerAddress();

        uri = new URI(address);
        String scheme;
        String port;
        if (uri.getScheme().equals("https")){
            scheme = "wss://";
        } else {
            scheme = "ws://";
        }
        if (uri.getPort() == -1){
            port = "";
        } else {
            port = ":" + uri.getPort();
        }
        return new URI(scheme + uri.getHost() + port + "/api/activity-updates");
    }

    @SuppressLint("Range")
    public String getDeviceUuid() {
        SQLiteDatabase db = getReadableDatabase();
        String uuid;

        Cursor cursor = db.rawQuery("SELECT * FROM SETTINGS",null);
        if(cursor.moveToFirst()) {
            try {
                uuid = cursor.getString(cursor.getColumnIndex(COLUMN_DEVICE_UUID));
                cursor.close();
            } catch (CursorIndexOutOfBoundsException e) {
                Log.e(TAG, "Failed to get uuid");
                uuid = "";
            }
        } else {
            Log.e(TAG, "Failed to get uuid");
            uuid = "";
        }
        return uuid;
    }
}
