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
    TextView tvDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(28);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.GRAY);
        tvStatus.setPadding(10, 10, 10, 10);

        etInput = new EditText(this);
        etInput.setHint("扫码或输入二维码");
        etInput.setTextSize(18);

        tvDetail = new TextView(this);
        tvDetail.setText("等待扫码...");
        tvDetail.setTextSize(16);

        layout.addView(tvStatus);
        layout.addView(etInput);
        layout.addView(tvDetail);

        setContentView(layout);

        // 扫码枪回车触发
        etInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                process();
                return true;
            }
            return false;
        });
    }

    private void process() {
        String input = etInput.getText().toString().trim();

        if (input.isEmpty()) {
            update(false, "❌ 空数据");
            return;
        }

        String extracted = extract(input);
        String[] parts = extracted.split("#");

        if (parts.length != 3) {
            update(false, "❌ 格式错误（必须3段）");
            return;
        }

        String prefix = parts[0];
        if (prefix.length() < 10) {
            update(false, "❌ 长度不足");
            return;
        }

        String id = prefix.substring(0, 1);
        String sku = prefix.substring(1, 10);
        String batch = prefix.substring(10);
        String pdStr = parts[1];
        String ddStr = parts[2];

        List<String> errors = new ArrayList<>();

        // ID
        if (!id.equals("0")) {
            errors.add("ID必须为0");
        }

        // SKU
        if (!sku.matches("\\d{9}")) {
            errors.add("SKU必须9位数字");
        }

        // Batch
        if (!batch.matches("[A-Za-z0-9]{1,15}")) {
            errors.add("Batch不合法（≤15位字母数字）");
        }

        // 日期处理
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        Date pd = parseDate(pdStr);
        Date dd = parseDate(ddStr);

        if (pd == null) {
            errors.add("生产日期格式错误");
        } else if (pd.after(today)) {
            errors.add("生产日期不能大于今天");
        }

        if (dd == null) {
            errors.add("到期日期格式错误");
        } else if (!dd.after(today)) {
            errors.add("到期日期必须大于今天");
        }

        if (pd != null && dd != null && !pd.before(dd)) {
            errors.add("生产日期必须小于到期日期");
        }

        // 输出
        if (errors.isEmpty()) {
            update(true, "✅ 校验通过");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String e : errors) {
                sb.append("❌ ").append(e).append("\n");
            }
            update(false, sb.toString());
        }

        etInput.setText("");
    }

    // ✅ Extract 逻辑（Excel G1）
    private String extract(String input) {

        if (input.contains("cii1/")) {
            int start = input.indexOf("cii1/") + 5;
            String raw = input.substring(start);

            if (raw.length() > 4) {
                raw = raw.substring(0, raw.length() - 4);
            }

            return raw.replace("&", "#");
        } else {
            return input.replace("&", "#");
        }
    }

    // ✅ 日期解析
    private Date parseDate(String s) {
        try {
            return new SimpleDateFormat("yyyyMMdd").parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void update(boolean pass, String msg) {

        if (pass) {
            tvStatus.setText("✅ PASS");
            tvStatus.setBackgroundColor(Color.parseColor("#00C853"));
        } else {
            tvStatus.setText("❌ FAIL");
            tvStatus.setBackgroundColor(Color.parseColor("#D50000"));
        }

        tvDetail.setText(msg);
    }
}
