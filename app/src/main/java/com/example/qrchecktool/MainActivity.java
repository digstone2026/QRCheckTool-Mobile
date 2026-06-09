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
    TextView tvStatus;
    TextView tvExtracted;
    LinearLayout tableContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20,20,20,20);

        // ✅ 状态条
        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(26);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.DKGRAY);
        tvStatus.setPadding(20,20,20,20);

        // ✅ 输入（不清空）
        etInput = new EditText(this);
        etInput.setHint("扫码枪输入");

        // ✅ 提取后内容
        tvExtracted = new TextView(this);
        tvExtracted.setTextSize(16);
        tvExtracted.setPadding(10,10,10,10);

        // ✅ 表格区域
        tableContainer = new LinearLayout(this);
        tableContainer.setOrientation(LinearLayout.VERTICAL);

        root.addView(tvStatus);
        root.addView(etInput);
        root.addView(tvExtracted);
        root.addView(tableContainer);

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

    private void process(String input){

        tableContainer.removeAllViews();

        if(input == null || input.isEmpty()){
            setStatus(false);
            return;
        }

        String extracted = extract(input);
        tvExtracted.setText("提取后内容：\n" + extracted);

        String[] parts = extracted.split("#");
        if(parts.length != 3){
            setStatus(false);
            return;
        }

        String prefix = parts[0];

        String id = prefix.substring(0,1);
        String sku = prefix.substring(1,10);
        String batch = prefix.substring(10);

        String sep1 = "#";
        String sep2 = "#";

        String pdStr = parts[1];
        String ddStr = parts[2];

        Date today = new Date();
        Date pd = parseDate(pdStr);
        Date dd = parseDate(ddStr);

        boolean idOK = id.equals("0");
        boolean skuOK = sku.matches("\\d{9}");
        boolean batchOK = batch.matches("[A-Za-z0-9]{1,15}");
        boolean pdOK = pd != null && !pd.after(today);
        boolean ddOK = dd != null && dd.after(today);
        boolean relationOK = pd != null && dd != null && pd.before(dd);

        boolean allOK = idOK && skuOK && batchOK && pdOK && ddOK && relationOK;

        setStatus(allOK);

        // ✅ 表格头
        addHeader();

        // ✅ 每一行（完全对齐你Excel）
        addRow("Identification Number", id, "默认数字0", idOK);
        addRow("SKU Number", sku, "9位数字", skuOK);
        addRow("Batch Number", batch, "≤15位字母或数字", batchOK);

        addRow("Separator", sep1, "#分隔符", true);

        addRow("Production Date", pdStr,
                "8位数字 YYYYMMDD 且 ≤ Today",
                pdOK);

        addRow("Separator", sep2, "#分隔符", true);

        addRow("Due Date", ddStr,
                "8位数字 YYYYMMDD 且 > Today",
                ddOK);

        addRow("Date Rule", "",
                "Production Date < Due Date",
                relationOK);
    }

    // ✅ 表头
    private void addHeader(){
        addRow("检查字段", "二维码字段内容", "校验规则", true, true);
    }

    // ✅ 表格行（带颜色）
    private void addRow(String col1, String col2, String col3, boolean pass){
        addRow(col1, col2, col3, pass, false);
    }

    private void addRow(String col1, String col2, String col3,
                        boolean pass, boolean isHeader){

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        row.addView(cell(col1, isHeader));
        row.addView(cell(col2, false));
        row.addView(cell(col3, false));
        row.addView(resultCell(pass, isHeader));

        tableContainer.addView(row);
    }

    private TextView cell(String text, boolean header){
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(10,10,10,10);
        tv.setWidth(250);

        if(header){
            tv.setBackgroundColor(Color.LTGRAY);
        }

        return tv;
    }

    private TextView resultCell(boolean pass, boolean header){
        TextView tv = new TextView(this);
        tv.setPadding(10,10,10,10);
        tv.setWidth(200);

        if(header){
            tv.setText("校验结果");
            tv.setBackgroundColor(Color.LTGRAY);
        }else{
            tv.setText(pass ? "TRUE" : "FALSE");
            tv.setBackgroundColor(pass ?
                    Color.parseColor("#00C853") :
                    Color.parseColor("#D50000"));
            tv.setTextColor(Color.WHITE);
        }

        return tv;
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
