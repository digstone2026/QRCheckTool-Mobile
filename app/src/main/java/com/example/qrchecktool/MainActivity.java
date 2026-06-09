package com.example.qrchecktool;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.*;

public class MainActivity extends Activity {

    EditText etInput;
    TextView tvStatus;
    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        // ✅ 状态栏
        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(26);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.GRAY);
        tvStatus.setPadding(10, 10, 10, 10);

        // ✅ 输入框（扫码枪输入）
        etInput = new EditText(this);
        etInput.setHint("扫码或输入二维码内容");
        etInput.setTextSize(18);

        // ✅ 结果
        tvResult = new TextView(this);
        tvResult.setText("等待扫码...");
        tvResult.setTextSize(20);

        layout.addView(tvStatus);
        layout.addView(etInput);
        layout.addView(tvResult);

        setContentView(layout);

        // ✅ 扫码枪 Enter 触发
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
            update(false, "空数据");
            return;
        }

        // ✅ 简单规则：必须包含 #
        if (input.contains("#")) {
            update(true, "格式正确");
        } else {
            update(false, "必须包含分隔符 #");
        }

        etInput.setText("");
    }

    private void update(boolean pass, String msg) {

        if (pass) {
            tvStatus.setText("✅ PASS");
            tvStatus.setBackgroundColor(Color.parseColor("#00C853"));
        } else {
            tvStatus.setText("❌ FAIL");
            tvStatus.setBackgroundColor(Color.parseColor("#D50000"));
        }

        tvResult.setText(msg);
    }
}
