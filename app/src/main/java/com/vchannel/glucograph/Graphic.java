package com.vchannel.glucograph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;

/**
 * Created by sseitov on 01.01.15.
 */
public class Graphic extends View {

    DataSource dataSource;
    BloodValue values[];
    double maxValue;
    double minValue;

    public int currentMonth;

    public Graphic(Context context, DataSource dataSource) {
        super(context);
        this.dataSource = dataSource;
    }

    public void setMonthValues(int year, int month) {
        currentMonth = month;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);

        int days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        values = new BloodValue[days];

        maxValue = 0; minValue = 34.;
        for (int day=0; day < days; day++) {
            BloodValue value = dataSource.valueForDate(year, month, day+1);
            if (value.morning > 0) {
                if (value.morning > maxValue) maxValue = value.morning;
                if (value.morning < minValue) minValue = value.morning;
            }
            if (value.evening > 0) {
                if (value.evening > maxValue) maxValue = value.evening;
                if (value.evening < minValue) minValue = value.evening;
            }
            values[day] = value;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth()-70;
        int h = getHeight()-60;

        Point org = new Point(50, 20+h);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawPaint(paint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(24);

        canvas.drawLine(org.x, org.y, org.x + w, org.y, paint);

        float deltaX = ((float)w)/15;
        Integer day = 1;
        for (int i=0; i<16; i++) {
            float x = org.x+deltaX*i;
            canvas.drawLine(x, org.y, x, org.y-h, paint);
            if (day <= values.length) {
                canvas.drawText(day.toString(), x - 10, org.y + 30, paint);
            } else {
                day--;
                canvas.drawText(day.toString(), x - 10, org.y + 30, paint);
                break;
            }
            day += 2;
        }

        if (maxValue > 0) {
            float deltaY = ((float)h)/10;
            double deltaValueY = (maxValue - minValue)/9.0;
            double precision = deltaY/deltaValueY;

            for (int i=1; i<11; i++) {
                float y = org.y-deltaY*i;
                canvas.drawLine(org.x, y, org.x+w, y, paint);
                Double value = minValue + deltaValueY*(i-1);
                canvas.drawText(String.format("%.1f", value), org.x - 45, y+10, paint);
            }

            paint.setStrokeWidth(10);

            ArrayList<Point> points = new ArrayList<Point>();
            for (int i=0; i<values.length; i++) {
                BloodValue v = values[i];
                if (v.morning > 0) {
                    Point pt = new Point((int)(org.x + deltaX/2*i), (int)(org.y - deltaY-(v.morning - minValue)*precision));
                    points.add(pt);
                }
            }
            if (points.size() > 1) {
                paint.setColor(getResources().getColor(R.color.morning));
                for (int i=0; i<points.size()-1; i++) {
                    Point pt1 = points.get(i);
                    Point pt2 = points.get(i+1);
                    canvas.drawLine(pt1.x, pt1.y, pt2.x, pt2.y, paint);
                }
            }

            points.clear();

            for (int i=0; i<values.length; i++) {
                BloodValue v = values[i];
                if (v.evening > 0) {
                    Point pt = new Point((int)(org.x + deltaX/2*i), (int)(org.y - deltaY-(v.evening - minValue)*precision));
                    points.add(pt);
                }
            }
            if (points.size() > 1) {
                paint.setColor(getResources().getColor(R.color.evening));
                for (int i=0; i<points.size()-1; i++) {
                    Point pt1 = points.get(i);
                    Point pt2 = points.get(i+1);
                    canvas.drawLine(pt1.x, pt1.y, pt2.x, pt2.y, paint);
                }
            }
/*
            path = new Path();
            paint.setColor(getResources().getColor(R.color.evening));
            canvas.drawPath(path, paint);*/
        }

    }
}
