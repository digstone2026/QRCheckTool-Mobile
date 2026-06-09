package com.example.qrchecktool;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    EditText etInput;
    Button btnScan;
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

        // 输入
        etInput = new EditText(this);
        etInput.setHint("扫码枪输入");

        // 摄像头按钮
        btnScan = new Button(this);
        btnScan.setText("📷 摄像头扫码");

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        tvSummary = new TextView(this);
        tvSummary.setPadding(10,20,10,10);

        root.addView(tvStatus);
        root.addView(etInput);
        root.addView(btnScan);
        root.addView(listContainer);
        root.addView(tvSummary);

        scroll.addView(root);
        setContentView(scroll);

        // ✅ 扫码枪 Enter 自动处理
        etInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == KeyEvent.ACTION_DOWN) {
                process(etInput.getText().toString());
                return true;
            }
            return false;
        });

        // ✅ 摄像头扫码（稳定方案）
        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            startActivityForResult(intent, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String result = data.getStringExtra("SCAN_RESULT");
            process(result);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void process(String input) {

        listContainer.removeAllViews();
        tvSummary.setText("");

        if (input == null || input.isEmpty()) {
            setStatus(false);
            tvSummary.setText("❌ 空数据");
            return;
        }

        String extracted = extract(input);
        String[] parts = extracted.split("#");

        if (parts.length != 3) {
            setStatus(false);
            tvSummary.setText("❌ 格式错误（必须3段）");
            return;
        }

        String prefix = parts[0];

        if (prefix.length() < 10) {
            setStatus(false);
            tvSummary.setText("❌ 长度不足");
            return;
        }

        String id = prefix.substring(0,1);
        String sku = prefix.substring(1,10);
        String batch = prefix.substring(10);

        String pdStr = parts[1];
        String ddStr = parts[2];

        List<String> errors = new ArrayList<>();

        addItem("ID", id.equals("0"), errors);
        addItem("SKU", sku.matches("\\d{9}"), errors);
        addItem("Batch", batch.matches("[A-Za-z0-9]{1,15}"), errors);

        Date today = new Date();
        Date pd = parseDate(pdStr);
        Date dd = parseDate(ddStr);

        addItem("Production Date", pd != null && !pd.after(today), errors);
        addItem("Due Date", dd != null && dd.after(today), errors);

        if (pd != null && dd != null && !pd.before(dd)) {
            errors.add("日期关系错误");
        }

        if (errors.isEmpty()) {
            setStatus(true);
            tvSummary.setText("✅ 全部通过");
        } else {
            setStatus(false);

            StringBuilder sb = new StringBuilder("❌ 错误：\n");
            for (String e : errors) {
                sb.append("• ").append(e).append("\n");
            }
            tvSummary.setText(sb.toString());
        }

        etInput.setText("");
    }

    private void addItem(String name, boolean pass, List<String> errors){
        TextView tv = new TextView(this);

        if(pass){
            tv.setText("✅ " + name);
        }else{
            tv.setText("❌ " + name);
            errors.add(name);
        }

        listContainer.addView(tv);
    }

    private void setStatus(boolean pass){

        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        if(pass){
            tvStatus.setText("✅ PASS");
            tvStatus.setBackgroundColor(Color.parseColor("#00C853"));
            tone.startTone(ToneGenerator.TONE_PROP_BEEP);
        }else{
            tvStatus.setText("❌ FAIL");
            tvStatus.setBackgroundColor(Color.parseColor("#D50000"));
            tone.startTone(ToneGenerator.TONE_SUP_ERROR);
        }
    }

    private String extract(String input){
        if(input.contains("cii1/")){
            String raw = input.substring(input.indexOf("cii1/")+5);
            return raw.replace("&","#").trim();
        }
        return input.replace("&","#").trim();
    }

    private Date parseDate(String s){
        try{
            return new SimpleDateFormat("yyyyMMdd").parse(s);
        }catch(Exception e){
            return null;
        }
    }
}
