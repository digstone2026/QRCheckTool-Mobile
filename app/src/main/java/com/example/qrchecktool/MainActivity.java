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
    TextView tvRaw;
    TextView tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);

        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(28);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.GRAY);
        tvStatus.setPadding(20, 20, 20, 20);

        etInput = new EditText(this);
        etInput.setHint("扫码输入");

        tvRaw = new TextView(this);
        tvRaw.setPadding(10, 10, 10, 10);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        tvSummary = new TextView(this);
        tvSummary.setTextSize(16);
        tvSummary.setPadding(10, 20, 10, 10);

        root.addView(tvStatus);
        root.addView(etInput);
        root.addView(tvRaw);
        root.addView(listContainer);
        root.addView(tvSummary);

        scroll.addView(root);
        setContentView(scroll);

        etInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                process();
                return true;
            }
            return false;
        });
    }

    private void process() {
        listContainer.removeAllViews();
        tvSummary.setText("");

        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) {
            setStatus(false);
            tvSummary.setText("❌ 空数据");
            return;
        }

        String extracted = extract(input);
        tvRaw.setText("原始数据:\n" + extracted);

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

        String id = prefix.substring(0, 1);
        String sku = prefix.substring(1, 10);
        String batch = prefix.substring(10);
        String pdStr = parts[1];
        String ddStr = parts[2];

        List<String> errors = new ArrayList<>();

        // ID
        addItem("Identification Number", id, "必须为0",
                id.equals("0"), "必须为0", errors);

        // SKU
        boolean skuValid = sku.matches("\\d{9}");
        addItem("SKU Number", sku, "9位数字",
                skuValid, "必须9位数字", errors);

        // Batch
        boolean batchValid = batch.matches("[A-Za-z0-9]{1,15}");
        addItem("Batch Number", batch, "≤15位字母数字",
                batchValid, "长度或字符不合法", errors);

        Date today = new Date();
        Date pd = parseDate(pdStr);
        Date dd = parseDate(ddStr);

        boolean pdValid = pd != null && !pd.after(today);
        addItem("Production Date", pdStr, "≤今天",
                pdValid, "格式错误或大于今天", errors);

        boolean ddValid = dd != null && dd.after(today);
        addItem("Due Date", ddStr, ">今天",
                ddValid, "必须大于今天", errors);

        if (pd != null && dd != null) {
            boolean relation = pd.before(dd);
            addItem("Date Relation", "",
                    "Production < Due",
                    relation, "生产日期必须小于到期日期", errors);
        }

        if (errors.isEmpty()) {
            setStatus(true);
            tvSummary.setText("✅ 全部通过");
        } else {
            setStatus(false);

            StringBuilder sb = new StringBuilder("❌ 错误总览：\n");
            for (String e : errors) {
                sb.append("• ").append(e).append("\n");
            }
            tvSummary.setText(sb.toString());
        }

        etInput.setText("");
    }

    private void addItem(String name, String value, String rule,
                         boolean pass, String errorMsg, List<String> errors) {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 20, 20, 20);
        box.setBackgroundColor(Color.parseColor("#F5F5F5"));

        TextView title = new TextView(this);
        title.setText((pass ? "✅ " : "❌ ") + name + ": " + value);

        TextView ruleView = new TextView(this);
        ruleView.setText("规则: " + rule);

        box.addView(title);
        box.addView(ruleView);

        if (!pass) {
            TextView err = new TextView(this);
            err.setText("错误: " + errorMsg);
            err.setTextColor(Color.RED);
            box.addView(err);
            errors.add(name);
        }

        listContainer.addView(box);
    }

    private void setStatus(boolean pass) {
        if (pass) {
            tvStatus.setText("✅ PASS");
            tvStatus.setBackgroundColor(Color.parseColor("#00C853"));
        } else {
            tvStatus.setText("❌ FAIL");
            tvStatus.setBackgroundColor(Color.parseColor("#D50000"));
        }
    }

    private String extract(String input) {
        if (input.contains("cii1/")) {
            int start = input.indexOf("cii1/") + 5;
            String raw = input.substring(start);
            if (raw.length() > 4) {
                raw = raw.substring(0, raw.length() - 4);
            }
            return raw.replace("&", "#");
        }
        return input.replace("&", "#");
    }

    private Date parseDate(String s) {
        try {
            return new SimpleDateFormat("yyyyMMdd").parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
