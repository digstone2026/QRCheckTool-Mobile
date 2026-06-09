package com.example.qrchecktool;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.*;

import com.journeyapps.barcodescanner.IntentIntegrator;
import com.journeyapps.barcodescanner.IntentResult;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    EditText etInput;
    Button btnCamera;
    TextView tvStatus;
    LinearLayout listContainer;
    TextView tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20,20,20,20);

        // 状态栏
        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(28);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.GRAY);
        tvStatus.setPadding(20,20,20,20);

        // 输入框（扫码枪）
        etInput = new EditText(this);
        etInput.setHint("扫码枪输入");

        // 摄像头按钮
        btnCamera = new Button(this);
        btnCamera.setText("📷 摄像头扫码");

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        tvSummary = new TextView(this);
        tvSummary.setPadding(10,20,10,10);

        root.addView(tvStatus);
        root.addView(etInput);
