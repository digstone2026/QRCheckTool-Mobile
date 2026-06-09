package com.example.qrchecktool;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    EditText etInput;
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

        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(28);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.GRAY);
        tvStatus.setPadding(20,20,20,20);

        etInput = new EditText(this);
        etInput.setHint("扫码枪输入");

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        tvSummary = new TextView(this);
        tvSummary.setPadding(10,20,10,10);

        root.addView(tvStatus);
        root.addView(etInput);
        root.addView(listContainer);
        root.addView(tvSummary);

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
            tvSummary.setText("❌ 格式错误");
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
        } else {
            tv.setText("❌ " + name);
            errors.add(name);
        }

        listContainer.addView(tv);
    }

    private void setStatus(boolean pass){
        if(pass){
            tvStatus.setText("✅ PASS");
            tvStatus.setBackgroundColor(Color.GREEN);
        }else{
            tvStatus.setText("❌ FAIL");
            tvStatus.setBackgroundColor(Color.RED);
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
