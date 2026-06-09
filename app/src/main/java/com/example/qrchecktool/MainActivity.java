package com.example.qrchecktool;package com.example.qimport android.view.KeyEvent;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    EditText etInput;
    TextView tvStatus;
    TextView tvExtracted;
    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20,20,20,20);

        // ✅ 顶部栏（状态 + 关闭按钮）
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);

        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(20);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.GRAY);
        tvStatus.setPadding(20,20,20,20);

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,1);

        tvStatus.setLayoutParams(statusParams);

        TextView btnClose = new TextView(this);
        btnClose.setText("❌");
        btnClose.setTextSize(20);
        btnClose.setPadding(20,20,20,20);
        btnClose.setGravity(Gravity.CENTER);

        btnClose.setOnClickListener(v -> showExitDialog());

        topBar.addView(tvStatus);
        topBar.addView(btnClose);

        etInput = new EditText(this);
        etInput.setHint("扫码输入");

        tvExtracted = new TextView(this);
        tvResult = new TextView(this);

        root.addView(topBar);
        root.addView(etInput);
        root.addView(tvExtracted);
        root.addView(tvResult);

        scroll.addView(root);
        setContentView(scroll);

        etInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
                process(etInput.getText().toString());
                return true;
            }
            return false;
        });
    }

    // ✅ 退出确认弹窗
    private void showExitDialog(){
        new AlertDialog.Builder(this)
                .setTitle("确认退出")
                .setMessage("是否退出程序？")
                .setPositiveButton("确认", (dialog, which) -> finish())
                .setNegativeButton("取消", null)
                .show();
    }

    private void process(String input){

        if(input == null || input.isEmpty()){
            setStatus(false);
            return;
        }

        String extracted = extract(input);
        tvExtracted.setText("提取后内容：\n" + extracted + "\n");

        String[] parts = extracted.split("#");
        if(parts.length != 3){
            setStatus(false);
            tvResult.setText("格式错误");
            return;
        }

        String prefix = parts[0];
        String id = prefix.substring(0,1);
        String sku = prefix.substring(1,10);
        String batch = prefix.substring(10);

        String pdStr = parts[1];
        String ddStr = parts[2];

        Date today = new Date();
        Date pd = strictDate(pdStr);
        Date dd = strictDate(ddStr);

        boolean idOK = id.equals("0");
        boolean skuOK = sku.matches("\\d{9}");
        boolean batchOK = batch.matches("[A-Za-z0-9]{1,15}");
        boolean pdOK = pd != null && !pd.after(today);
        boolean ddOK = dd != null && dd.after(today);
        boolean relationOK = pd != null && dd != null && pd.before(dd);

        boolean allOK = idOK && skuOK && batchOK && pdOK && ddOK && relationOK;

        setStatus(allOK);

        String line = "----------------------------------------\n";

        StringBuilder sb = new StringBuilder();

        sb.append(buildBlock("Identification Number", id,
                "1", "必须为0", idOK,
                idOK ? "" : "必须为0"));

        sb.append(line);
        sb.append(buildBlock("SKU Number", sku,
                String.valueOf(sku.length()),
                "9位数字", skuOK,
                skuOK ? "" : "必须9位数字"));

        sb.append(line);
        sb.append(buildBlock("Batch Number", batch,
                String.valueOf(batch.length()),
                "≤15位字母数字", batchOK,
                batchOK ? "" : "非法"));

        sb.append(line);
        sb.append(buildBlock("Production Date", pdStr,
                String.valueOf(pdStr.length()),
                "YYYYMMDD 且 ≤ Today", pdOK,
                pd == null ? "非法日期" :
                        pd.after(today) ? "大于今天" : ""));

        sb.append(line);
        sb.append(buildBlock("Due Date", ddStr,
                String.valueOf(ddStr.length()),
                "YYYYMMDD 且 > Today", ddOK,
                dd == null ? "非法日期" :
                        !dd.after(today) ? "必须大于今天" : ""));

        sb.append(line);
        sb.append(buildBlock("Date Relation","",
                "-","Production < Due",
                relationOK,
                relationOK ? "" : "生产日期必须小于到期日期"));

        tvResult.setText(sb.toString());
    }

    private String buildBlock(String name, String value,
                              String length, String rule,
                              boolean pass, String error){

        StringBuilder s = new StringBuilder();

        s.append("[").append(name).append("]\n");
        s.append("二维码内容：").append(value).append("\n");
        s.append("长度：").append(length).append("\n");
        s.append("校验规则：").append(rule).append("\n");
        s.append("校验结果：").append(pass ? "TRUE" : "FALSE").append("\n");

        if(!pass && error != null && !error.isEmpty()){
            s.append("错误信息：").append(error).append("\n");
        }

        return s.toString();
    }

    private void setStatus(boolean pass){
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC,100);

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

    private Date strictDate(String s){
        if(!s.matches("\\d{8}")) return null;
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            sdf.setLenient(false);
            return sdf.parse(s);
        }catch(Exception e){
            return null;
        }
    }
}


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Gravity;
