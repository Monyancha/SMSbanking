package com.khizhny.smsbanking;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TimePicker;

import java.util.Calendar;

class SetTime implements View.OnFocusChangeListener, TimePickerDialog.OnTimeSetListener {

		private EditText editText;
		private Calendar myCalendar;
		private Context ctx;

		public SetTime(EditText editText, Context ctx){
				this.editText = editText;
				this.ctx=ctx;
				this.editText.setOnFocusChangeListener(this);
				this.myCalendar = Calendar.getInstance();

		}

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
				// TODO Auto-generated method stub
				if(hasFocus){
						int hour = myCalendar.get(Calendar.HOUR_OF_DAY);
						int minute = myCalendar.get(Calendar.MINUTE);
						new TimePickerDialog(ctx, this, hour, minute, true).show();
				}
		}

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				// TODO Auto-generated method stub
				this.editText.setText( hourOfDay + ":" + minute);
		}

}