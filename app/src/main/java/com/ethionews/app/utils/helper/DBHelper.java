package com.ethionews.app.utils.helper;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ethionews.app.BuildConfig;

import com.ethionews.app.callback.Callback;
import com.ethionews.app.item.ItemAbout;
import com.ethionews.app.item.ItemLiveTv;
import com.ethionews.app.utils.EncryptData;

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = BuildConfig.APPLICATION_ID + "_" + "app.db";
    private final SQLiteDatabase db;
    EncryptData encryptData;
    final Context context;

    private static final String TABLE_ABOUT = "about";
    private static final String TABLE_RECENT = "recent";

    private static final String TAG_ID = "id";

    private static final String TAG_LIVE_ID = "live_id";
    private static final String TAG_TITLE = "live_title";
    private static final String TAG_IMAGE = "image";
    private static final String TAG_PREMIUM = "is_premium";

    private static final String TAG_ABOUT_EMAIL = "email";
    private static final String TAG_ABOUT_AUTHOR = "author";
    private static final String TAG_ABOUT_CONTACT = "contact";
    private static final String TAG_ABOUT_WEBSITE = "website";
    private static final String TAG_ABOUT_DESC = "description";
    private static final String TAG_ABOUT_DEVELOPED = "developed";
    private static final String TAG_ABOUT_MORE_APP = "more_apps";

    private final String[] columnsAbout = new String[]{
            TAG_ABOUT_EMAIL, TAG_ABOUT_AUTHOR, TAG_ABOUT_CONTACT, TAG_ABOUT_WEBSITE, TAG_ABOUT_DESC,
            TAG_ABOUT_DEVELOPED, TAG_ABOUT_MORE_APP
    };

    private final String[] columnsRecent = new String[]{TAG_ID, TAG_LIVE_ID, TAG_TITLE, TAG_IMAGE, TAG_PREMIUM};

    // Creating table about
    private static final String CREATE_TABLE_ABOUT = "CREATE TABLE " + TABLE_ABOUT + " ("
            + TAG_ABOUT_EMAIL + " TEXT,"
            + TAG_ABOUT_AUTHOR + " TEXT,"
            + TAG_ABOUT_CONTACT + " TEXT,"
            + TAG_ABOUT_WEBSITE + " TEXT,"
            + TAG_ABOUT_DESC + " TEXT,"
            + TAG_ABOUT_DEVELOPED + " TEXT,"
            + TAG_ABOUT_MORE_APP + " TEXT"
            + ")";


    // Creating table query
    private static final String CREATE_TABLE_RECENT = "create table " + TABLE_RECENT + "(" +
            TAG_ID + " integer PRIMARY KEY AUTOINCREMENT," +
            TAG_LIVE_ID + " TEXT," +
            TAG_TITLE + " TEXT," +
            TAG_IMAGE + " TEXT," +
            TAG_PREMIUM + " TEXT);";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        encryptData = new EncryptData(context);
        this.context = context;
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ABOUT);
        db.execSQL(CREATE_TABLE_RECENT);
    }

    // Upgrade -------------------------------------------------------------------------------------
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECENT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ABOUT);
        onCreate(db);
    }

    @SuppressLint("Range")
    public void addToRecent(ItemLiveTv itemData) {
        try (Cursor cursor = db.query(TABLE_RECENT, columnsRecent, null, null, null, null, null)) {
            if (cursor != null && cursor.getCount() > 20) {
                cursor.moveToFirst();
                String oldestId = cursor.getString(cursor.getColumnIndex(TAG_LIVE_ID));
                db.delete(TABLE_RECENT, TAG_LIVE_ID + "=?", new String[]{oldestId});
            }
        }

        if (Boolean.TRUE.equals(checkRecent(itemData.getId()))) {
            db.delete(TABLE_RECENT, TAG_LIVE_ID + "=" + itemData.getId(), null);
        }

        String name = itemData.getLiveTitle().replace("'", "%27");
        String imageBig = encryptData.encrypt(itemData.getImage().replace(" ", "%20"));

        ContentValues contentValues = new ContentValues();
        contentValues.put(TAG_LIVE_ID, itemData.getId());
        contentValues.put(TAG_TITLE, name);
        contentValues.put(TAG_IMAGE, imageBig);
        contentValues.put(TAG_PREMIUM, itemData.getIsPremium());

        db.insert(TABLE_RECENT, null, contentValues);
    }

    private Boolean checkRecent(String id) {
        try (Cursor cursor = db.query(TABLE_RECENT, columnsRecent, TAG_LIVE_ID + "=?", new String[]{id}, null, null, null)) {
            return cursor.getCount() > 0;
        }
    }

    @SuppressLint("Range")
    public String getRecentIDs(String limit) {
        StringBuilder radioIDs = new StringBuilder();
        try (Cursor cursor = db.query(TABLE_RECENT, new String[]{TAG_LIVE_ID}, null, null, null, null, TAG_ID + " DESC", limit)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    if (radioIDs.length() > 0) {
                        radioIDs.append(",");
                    }
                    radioIDs.append(cursor.getString(cursor.getColumnIndex(TAG_LIVE_ID)));
                } while (cursor.moveToNext());
            }
        }
        return radioIDs.toString();
    }

    // About ---------------------------------------------------------------------------------------
    public void addToAbout() {
        try {
            db.delete(TABLE_ABOUT, null, null);

            ContentValues contentValues = new ContentValues();
            contentValues.put(TAG_ABOUT_EMAIL, Callback.getItemAbout().getEmail());
            contentValues.put(TAG_ABOUT_AUTHOR, Callback.getItemAbout().getAuthor());
            contentValues.put(TAG_ABOUT_CONTACT, Callback.getItemAbout().getContact());
            contentValues.put(TAG_ABOUT_WEBSITE, Callback.getItemAbout().getWebsite());
            contentValues.put(TAG_ABOUT_DESC, Callback.getItemAbout().getAppDesc());
            contentValues.put(TAG_ABOUT_DEVELOPED, Callback.getItemAbout().getDevelopedBY());
            db.insert(TABLE_ABOUT, null, contentValues);
        } catch (Exception e) {
            Log.e(TAG, "Error add to about", e);
        }
    }

    @SuppressLint("Range")
    public Boolean getAbout() {
        try (Cursor c = db.query(TABLE_ABOUT, columnsAbout, null, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String email = c.getString(c.getColumnIndex(TAG_ABOUT_EMAIL));
                String author = c.getString(c.getColumnIndex(TAG_ABOUT_AUTHOR));
                String contact = c.getString(c.getColumnIndex(TAG_ABOUT_CONTACT));
                String website = c.getString(c.getColumnIndex(TAG_ABOUT_WEBSITE));
                String desc = c.getString(c.getColumnIndex(TAG_ABOUT_DESC));
                String developed = c.getString(c.getColumnIndex(TAG_ABOUT_DEVELOPED));
                String moreApps = c.getString(c.getColumnIndex(TAG_ABOUT_MORE_APP));

                Callback.setItemAbout(new ItemAbout(email, author, contact, website, desc, developed, moreApps));
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void close () {
        if (db != null && db.isOpen()) {
            db.close();
            super.close();
        }
    }
}