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

        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(28);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.GRAY);
        tvStatus.setPadding(20,20,20,20);

        etInput = new EditText(this);
        etInput.setHint("扫码枪输入");

        btnCamera = new Button(this);
        btnCamera.setText("📷 摄像头扫码");

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);

        tvSummary = new TextView(this);
        tvSummary.setPadding(10,20,10,10);

        root.addView(tvStatus);
        root.addView(etInput);
        root.addView(btnCamera);
        root.addView(listContainer);
        root.addView(tvSummary);

        scroll.addView(root);
        setContentView(scroll);

        // ✅ 扫码枪
        etInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
                process(etInput.getText().toString());
                return true;
            }
            return false;
        });

        // ✅ 摄像头扫码
        btnCamera.setOnClickListener(v ->
                new IntentIntegrator(this).initiateScan());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            process(result.getContents());
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

        addItem("Identification Number", id, "必须为0",
                id.equals("0"), "必须为0", errors);

        addItem("SKU Number", sku, "9位数字",
                sku.matches("\\d{9}"), "必须9位数字", errors);

        addItem("Batch Number", batch, "≤15位字母数字",
                batch.matches("[A-Za-z0-9]{1,15}"),
                "长度或字符错误", errors);

        Date today = new Date();
        Date pd = parseDate(pdStr);
        Date dd = parseDate(ddStr);

        addItem("Production Date", pdStr, "≤今天",
                pd != null && !pd.after(today),
                "格式错误或大于今天", errors);

        addItem("Due Date", ddStr, ">今天",
                dd != null && dd.after(today),
                "必须大于今天", errors);

        if (pd != null && dd != null) {
            boolean relation = pd.before(dd);
            addItem("Date Relation", "",
                    "PD < DD",
                    relation,
                    "生产日期必须小于过期日期",
                    errors);
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
                         boolean pass, String err, List<String> errors){

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20,20,20,20);
        box.setBackgroundColor(Color.parseColor("#F5F5F5"));

        TextView title = new TextView(this);
        title.setText((pass ? "✅ " : "❌ ") + name + ": " + value);

        TextView ruleView = new TextView(this);
        ruleView.setText("规则：" + rule);

        box.addView(title);
        box.addView(ruleView);

        if(!pass){
            TextView tvErr = new TextView(this);
            tvErr.setText("错误：" + err);
            tvErr.setTextColor(Color.RED);
            box.addView(tvErr);

            errors.add(name);
        }

        listContainer.addView(box);
    }

    private void setStatus(boolean pass){
        if(pass){
            tvStatus.setText("✅ PASS");
            tvStatus.setBackgroundColor(Color.parseColor("#00C853"));
        }else{
            tvStatus.setText("❌ FAIL");
            tvStatus.setBackgroundColor(Color.parseColor("#D50000"));
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
