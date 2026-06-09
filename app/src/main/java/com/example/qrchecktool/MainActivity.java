package com.example.qrchecktool;

import android.app.Activity;
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
    LinearLayout container;

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
        tvStatus.setTextSize(26);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.GRAY);
        tvStatus.setPadding(20,20,20,20);

        // 输入（不清空）
        etInput = new EditText(this);
        etInput.setHint("扫码输入");

        // 提取后内容
        tvExtracted = new TextView(this);
        tvExtracted.setPadding(10,10,10,10);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        root.addView(tvStatus);
        root.addView(etInput);
        root.addView(tvExtracted);
        root.addView(container);

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

        container.removeAllViews();

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

        String pdStr = parts[1];
        String ddStr = parts[2];

        boolean idOK = id.equals("0");
        boolean skuOK = sku.matches("\\d{9}");
        boolean batchOK = batch.matches("[A-Za-z0-9]{1,15}");

        Date today = new Date();

        Date pd = strictDate(pdStr);
        Date dd = strictDate(ddStr);

        boolean pdOK = pd != null && !pd.after(today);
        boolean ddOK = dd != null && dd.after(today);
        boolean relationOK = pd != null && dd != null && pd.before(dd);

        boolean allOK = idOK && skuOK && batchOK && pdOK && ddOK && relationOK;

        setStatus(allOK);

        addCard("Identification Number", id, "1", "必须为0",
                idOK, idOK ? "" : "必须为0");

        addCard("SKU Number", sku, String.valueOf(sku.length()), "9位数字",
                skuOK, skuOK ? "" : "必须为9位数字");

        addCard("Batch Number", batch, String.valueOf(batch.length()), "≤15位字母数字",
                batchOK, batchOK ? "" : "长度或字符非法");

        addCard("Production Date", pdStr,
                String.valueOf(pdStr.length()),
                "8位日期YYYYMMDD 且 ≤ Today",
                pdOK,
                pd == null ? "非法日期格式" :
                        pd.after(today) ? "大于今天" : "");

        addCard("Due Date", ddStr,
                String.valueOf(ddStr.length()),
                "8位日期YYYYMMDD 且 > Today",
                ddOK,
                dd == null ? "非法日期格式" :
                        !dd.after(today) ? "必须大于今天" : "");

        addCard("Date Relation", "",
                "-",
                "Production < Due",
                relationOK,
                relationOK ? "" : "生产日期需小于到期日期");
    }

    // ✅ 每个字段卡片
    private void addCard(String title, String value, String length,
                         String rule, boolean pass, String error){

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20,20,20,20);
        card.setBackgroundColor(Color.parseColor("#F5F5F5"));

        TextView t1 = new TextView(this);
        t1.setText("\n[" + title + "]");
        t1.setTextSize(16);

        TextView t2 = new TextView(this);
        t2.setText("二维码内容：" + value);

        TextView t3 = new TextView(this);
        t3.setText("长度：" + length);

        TextView t4 = new TextView(this);
        t4.setText("校验规则：" + rule);

        TextView t5 = new TextView(this);
        t5.setText("校验结果：" + (pass ? "TRUE" : "FALSE"));
        t5.setTextColor(pass ? Color.parseColor("#00C853") : Color.RED);

        card.addView(t1);
        card.addView(t2);
        card.addView(t3);
        card.addView(t4);
        card.addView(t5);

        if(!pass){
            TextView t6 = new TextView(this);
            t6.setText("错误信息：" + error);
            t6.setTextColor(Color.RED);
            card.addView(t6);
        }

        container.addView(card);
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

    // ✅ 严格日期校验（关键）
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
