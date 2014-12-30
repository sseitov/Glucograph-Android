package com.vchannel.glucograph;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

public class MainActivity extends Activity {

    private static final String appKey = "9yjxpsg3ck2b19d";
    private static final String appSecret = "ma6d3huqfcbpkbk";

    private static final int REQUEST_LINK_TO_DBX = 0;

    private TextView currentDateText;
    private CalendarView calendar;
    private BloodValue currentValue;

    private DbxAccountManager dbxAccountManager;

    void setCurrentDate(Date date) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        currentDateText.setText(df.format(date));
        calendar.setDate(date.getTime());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbxAccountManager = DbxAccountManager.getInstance(getApplicationContext(), appKey, appSecret);

        currentDateText = (TextView)findViewById(R.id.currentDate);

        calendar = (CalendarView)findViewById(R.id.calendarView);
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                GregorianCalendar c = new GregorianCalendar(year, month, dayOfMonth);
                setCurrentDate(c.getTime());
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        setCurrentDate(GregorianCalendar.getInstance().getTime());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if (dbxAccountManager.hasLinkedAccount()) {
                doSynchro();
            } else {
                dbxAccountManager.startLink((Activity)this, REQUEST_LINK_TO_DBX);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                doSynchro();
            } else {
                Toast t = Toast.makeText(this, "Link to Dropbox failed or was cancelled.", Toast.LENGTH_LONG);
                t.show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void doSynchro() {
        try {
            final String DB_FILE_NAME = "Glucograph2.sqlite";
            DbxPath dbxPath = new DbxPath(DbxPath.ROOT, DB_FILE_NAME);

            DbxFileSystem dbxFs = DbxFileSystem.forAccount(dbxAccountManager.getLinkedAccount());

            if (!dbxFs.exists(dbxPath)) {
                DbxFile testFile = dbxFs.create(dbxPath);
                try {
                    testFile.writeString("Hello Dropbox");
                } finally {
                    testFile.close();
                }
                return;
            }

            if (dbxFs.isFile(dbxPath)) {
                String resultData;
                DbxFile dbxFile = dbxFs.open(dbxPath);
                try {
                    FileInputStream inputStream = dbxFile.getReadStream();
                    byte[] mBuffer = new byte[1024];
                    int mLength;
                    int allBytes = 0;
                    while ((mLength = inputStream.read(mBuffer))>0)
                    {
                        allBytes += mLength;
//                        mOutput.write(mBuffer, 0, mLength);
                    }
//                    mOutput.flush();
//                    mOutput.close();
                    inputStream.close();

                    Toast t = Toast.makeText(this, "Do Dropbox synchro "+allBytes, Toast.LENGTH_LONG);
                    t.show();
                } finally {
                    dbxFile.close();
                }
            }
        } catch (IOException e) {
        }
    }
}
