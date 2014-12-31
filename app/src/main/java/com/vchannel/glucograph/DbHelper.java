package com.vchannel.glucograph;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by sseitov on 31.12.14.
 */
public class DbHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "bloods";

    public static final String DB_NAME = "Glucograph2.sqlite";
    private static final int DB_VERSION = 1;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_NAME+
                " (day timestamp NOT NULL PRIMARY KEY UNIQUE, morning float NOT NULL, evening float NOT NULL, comment text NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }
}
