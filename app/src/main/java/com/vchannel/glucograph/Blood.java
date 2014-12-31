package com.vchannel.glucograph;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.lang.reflect.Field;


public class Blood extends Activity {

    NumberPicker mainPicker;
    NumberPicker decimalPicker;
    EditText commentText;
    TextView valueText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood);

        // Get the message from the intent
        Intent intent = getIntent();
        boolean forMorning = intent.getBooleanExtra("morning", false);
        int color = forMorning ? getResources().getColor(R.color.morning) : getResources().getColor(R.color.evening);
        double value = intent.getDoubleExtra("value", 1.0);
        int main = (int)value;
        int decimal = (int)((value - (double)main)*10.);
        String comment = intent.getStringExtra("comment");

        valueText = (TextView)findViewById(R.id.valueText);
        valueText.setText(main + "." + decimal);
        valueText.setTextColor(color);

        mainPicker = (NumberPicker)findViewById(R.id.mainDigit);
        setNumberPickerTextColor(mainPicker, color);
        mainPicker.setMinValue(1);
        mainPicker.setMaxValue(33);
        mainPicker.setValue(main);
        mainPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                pickerChanged();
            }
        });

        decimalPicker = (NumberPicker)findViewById(R.id.decimalDigit);
        setNumberPickerTextColor(decimalPicker, color);
        decimalPicker.setMinValue(0);
        decimalPicker.setMaxValue(9);
        decimalPicker.setValue(decimal);
        decimalPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                pickerChanged();
            }
        });

        commentText = (EditText)findViewById(R.id.editText);
        commentText.setText(comment);

        Button setupButton = (Button)findViewById(R.id.setValue);
        setupButton.setTextColor(color);
        setupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setup();
            }
        });
    }

    void pickerChanged() {
        valueText.setText(mainPicker.getValue()+"."+decimalPicker.getValue());
    }

    private boolean setNumberPickerTextColor(NumberPicker numberPicker, int color)
    {
        final int count = numberPicker.getChildCount();
        for(int i = 0; i < count; i++){
            View child = numberPicker.getChildAt(i);
            if(child instanceof EditText){
                try{
                    Field selectorWheelPaintField = numberPicker.getClass()
                            .getDeclaredField("mSelectorWheelPaint");
                    selectorWheelPaintField.setAccessible(true);
                    ((Paint)selectorWheelPaintField.get(numberPicker)).setColor(color);
                    ((EditText)child).setTextColor(color);
                    numberPicker.invalidate();
                    return true;
                }
                catch(NoSuchFieldException e){
                }
                catch(IllegalAccessException e){
                }
                catch(IllegalArgumentException e){
                }
            }
        }
        return false;
    }

    void setup() {
        Intent intent = new Intent();
        double value = (double)mainPicker.getValue() + (double)decimalPicker.getValue()/10.;
        intent.putExtra("value", value);
        intent.putExtra("comment", commentText.getText());
        setResult(RESULT_OK, intent);
        finish();
    }
}
