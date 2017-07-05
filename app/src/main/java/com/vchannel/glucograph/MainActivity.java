package com.vchannel.glucograph;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainActivity extends Activity {

    private static final String appKey = "9yjxpsg3ck2b19d";
    private static final String appSecret = "ma6d3huqfcbpkbk";

    private static final int REQUEST_MORNING        = 1;
    private static final int REQUEST_EVENING        = 2;

    private TextView currentDateText;
    private CalendarView calendar;
    private Button morningButton;
    private Button eveningButton;
    private BloodValue currentValue;
    private Graphic graphic;

    private ProgressDialog progressDialog;

    private DataSource dataSource;
    private DropboxAPI<AndroidAuthSession> dbxApi;

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

        AppKeyPair appKeys = new AppKeyPair(appKey, appSecret);
        AndroidAuthSession session = new AndroidAuthSession(appKeys);
        dbxApi = new DropboxAPI<AndroidAuthSession>(session);

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

    private void showMessage(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (dbxApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                dbxApi.getSession().finishAuthentication();

                String accessToken = dbxApi.getSession().getOAuth2AccessToken();
                setAccessToken(accessToken);
            } catch (IllegalStateException e) {
                showMessage("Error DropBox authenticating");
            }
        }

        setCurrentValue();
        graphic.setMonthValues(currentValue.date.getYear(), currentValue.date.getMonth());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void setAccessToken(String token) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("accessToken", token);
        editor.commit();
    }

    private String getAccessToken() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        return preferences.getString("accessToken", null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if (dbxApi.getSession().isLinked()) {
                new SynchroTask().execute();
            } else {
                String token = getAccessToken();
                if (token != null) {
                    dbxApi.getSession().setOAuth2AccessToken(token);
                    new SynchroTask().execute();
                } else {
                    dbxApi.getSession().startOAuth2Authentication(MainActivity.this);
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MORNING) {
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

    private class SynchroTask extends AsyncTask<Void, Void, Void> {

        private DropboxAPI.Entry existingEntry;
        private DropboxAPI.DropboxFileInfo fileInfo;
        private String synchroError;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Show progressdialog
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle(R.string.app_name);
            progressDialog.setMessage("Synchronization...");
            progressDialog.setIndeterminate(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                FileOutputStream outputStream = new FileOutputStream(dataSource.dbPath());
                fileInfo = dbxApi.getFile("/"+DbHelper.DB_NAME, null, outputStream, null);
                //existingEntry = dbxApi.metadata("/"+DbHelper.DB_NAME, 1, null, false, null);
            } catch (IOException e) {
                synchroError = e.getMessage();
            } catch (DropboxException e) {
                synchroError = e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.hide();
            if (fileInfo != null) {
                showMessage(fileInfo.getMetadata().modified);
            } else if (synchroError != null) {
                showMessage(synchroError);
            }
        }
    }
    /*
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
    */
}
