package com.khizhny.smsbanking;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class TipActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final int tip_res_id = getIntent().getExtras().getInt("tip_res_id");
        TextView tipView = (TextView) findViewById(R.id.tipTextId);
        tipView.setText(tip_res_id);

        ImageButton closeButtonView = (ImageButton) findViewById(R.id.imageButton);
        closeButtonView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TipActivity.this.finish();
            }
        });
    }
}
