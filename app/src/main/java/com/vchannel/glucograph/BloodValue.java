package com.vchannel.glucograph;

import java.sql.Date;
import java.text.DateFormat;
import java.util.Locale;

/**
 * Created by sseitov on 29.12.14.
 */
public class BloodValue {

    public Date date;
    public Double morning;
    public Double evening;
    public String comment;

    BloodValue(Date date) {
        this.date = date;
        this.morning = 0.;
        this.evening = 0.;
    }

    BloodValue(Date date, double morning, double evening, String comment) {
        this.date = date;
        this.morning = morning;
        this.evening = evening;
        this.comment = comment;
    }

    String dateText() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault());
        return df.format(this.date);
    }

    String morning() {
        if (this.morning > 0) {
            return this.morning.toString();
        } else {
            return null;
        }
    }

    String evening() {
        if (this.evening > 0) {
            return this.evening.toString();
        } else {
            return null;
        }
    }
}
