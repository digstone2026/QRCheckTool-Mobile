package com.example.qrchecktool;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(this);
        tv.setText("✅ QR Tool 已启动");

        layout.addView(tv);

        setContentView(layout);
    }
}
