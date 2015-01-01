package com.vchannel.glucograph;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

public class MainActivity extends Activity {

    private static final String appKey = "9yjxpsg3ck2b19d";
    private static final String appSecret = "ma6d3huqfcbpkbk";

    private static final int REQUEST_LINK_TO_DBX    = 0;
    private static final int REQUEST_MORNING        = 1;
    private static final int REQUEST_EVENING        = 2;

    private TextView currentDateText;
    private CalendarView calendar;
    private Button morningButton;
    private Button eveningButton;
    private BloodValue currentValue;
    private Graphic graphic;

    private DataSource dataSource;
    private DbxAccountManager dbxAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataSource = new DataSource(this);
        dataSource.open();

        LinearLayout linearView = (LinearLayout) findViewById(R.id.graphicLayout);
        graphic = new Graphic(this, dataSource);
        linearView.addView(graphic);

        morningButton = (Button)findViewById(R.id.setMorning);
        morningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateValue(true);
            }
        });
        eveningButton = (Button)findViewById(R.id.setEvening);
        eveningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateValue(false);
            }
        });

        dbxAccountManager = DbxAccountManager.getInstance(getApplicationContext(), appKey, appSecret);

        currentDateText = (TextView)findViewById(R.id.currentDate);

        calendar = (CalendarView)findViewById(R.id.calendarView);
        calendar.setShowWeekNumber(false);
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                currentValue = getValueForDate(year, month, dayOfMonth);
                setCurrentValue();
                if (month != graphic.currentMonth) {
                    graphic.setMonthValues(year, month);
                }
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        setCurrentValue();
        graphic.setMonthValues(currentValue.date.getYear(), currentValue.date.getMonth());
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
                Toast.makeText(this, "Link to Dropbox succeded.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Link to Dropbox failed or was cancelled.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_MORNING) {
            if (data != null) {
                currentValue.comment = data.getStringExtra("comment");
                currentValue.morning = data.getDoubleExtra("value", currentValue.morning);
                setCurrentValue();
            }
        } else if (requestCode == REQUEST_EVENING) {
            if (data != null) {
                currentValue.comment = data.getStringExtra("comment");
                currentValue.evening = data.getDoubleExtra("value", currentValue.evening);
                setCurrentValue();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void updateValue(boolean forMorning)
    {
        Intent intent = new Intent(this, Blood.class);
        int request = forMorning ? REQUEST_MORNING : REQUEST_EVENING;
        if (forMorning) {
            if (currentValue.morning > 0) {
                intent.putExtra("value", currentValue.morning);
            }
        } else {
            if (currentValue.evening > 0) {
                intent.putExtra("value", currentValue.evening);
            }
        }
        intent.putExtra("morning", forMorning);
        intent.putExtra("comment", currentValue.comment);
        startActivityForResult(intent, request);
    }

    BloodValue getValueForDate(int year, int month, int day) {
        return dataSource.valueForDate(year, month, day);
    }

    void setCurrentValue() {
        if (currentValue == null) {
            Calendar c = GregorianCalendar.getInstance();
            currentValue = getValueForDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        }
        if (currentValue.morning() != null) {
            morningButton.setText(currentValue.morning());
        } else {
            morningButton.setText(R.string.set_morning);
        }
        if (currentValue.evening() != null) {
            eveningButton.setText(currentValue.evening());
        } else {
            eveningButton.setText(R.string.set_evening);
        }
        currentDateText.setText(currentValue.dateText());
    }

    void doSynchro() {
        try {
            DbxPath dbxPath = new DbxPath(DbxPath.ROOT, DbHelper.DB_NAME);
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(dbxAccountManager.getLinkedAccount());
            if (dbxFs.isFile(dbxPath)) {
                DbxFile dbxFile = dbxFs.open(dbxPath);
                try {
                    FileInputStream inputStream = dbxFile.getReadStream();
                    dataSource.close();
                    FileOutputStream outputStream = new FileOutputStream(dataSource.dbPath());
                    byte[] mBuffer = new byte[1024];
                    int mLength;
                    while ((mLength = inputStream.read(mBuffer)) > 0)
                    {
                        outputStream.write(mBuffer, 0, mLength);
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                } finally {
                    dbxFile.close();
                    dataSource.open();
                }
                Toast.makeText(this, "Dropbox synchronization succeded!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Dropbox synchronization error!", Toast.LENGTH_LONG).show();
        }
    }
}
