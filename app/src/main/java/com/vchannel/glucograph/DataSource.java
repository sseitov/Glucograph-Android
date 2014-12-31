package com.vchannel.glucograph;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import java.util.Calendar;
import java.sql.Date;

/**
 * Created by sseitov on 31.12.14.
 */
public class DataSource {

    private SQLiteDatabase database;
    private DbHelper dbHelper;

    public DataSource(Context context) {
        dbHelper = new DbHelper(context);
    }

    public String dbPath() {
        return Environment.getDataDirectory()+"/data/com.vchannel.glucograph/databases/"+dbHelper.DB_NAME;
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    private long returnSeconds(int year, int month, int day) {
        Calendar src = Calendar.getInstance();
        src.set(1970, 0, 1, 0, 0, 0);
        Calendar dst = Calendar.getInstance();
        dst.set(year, month, day, -2, 0, 0);
        long diff = dst.getTimeInMillis() - src.getTimeInMillis();
        long seconds = (diff / 1000);
        return seconds;
    }

    public BloodValue valueForDate(int year, int month, int day) {
        long seconds = returnSeconds(year, month, day);
        String q = "select * from bloods where day="+seconds;
        Cursor cursor = database.rawQuery(q, null);
        if (cursor.moveToFirst()) {
            Date date = new java.sql.Date(cursor.getLong(0)*1000);
            return (new BloodValue(date, cursor.getDouble(1), cursor.getDouble(2), cursor.getString(3)));
        } else {
            Date date = new Date(year, month, day);
            return (new BloodValue(date));
        }
    }
}
