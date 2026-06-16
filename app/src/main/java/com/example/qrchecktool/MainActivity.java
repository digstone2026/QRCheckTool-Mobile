package com.example.qrchecktool;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.*;

import com.journeyapps.barcodescanner.IntentIntegrator;
import com.journeyapps.barcodescanner.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    EditText etInput;
    TextView tvStatus, tvInfo, tvExtracted;
    LinearLayout container;

    int passCount = 0;
    int failCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ 动态申请摄像头权限（鸿蒙/安卓）
        if (checkSelfPermission(android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{android.Manifest.permission.CAMERA},
                    100
            );
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // ✅ 状态栏
        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(34);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTextColor(Color.WHITE);

        tvInfo = new TextView(this);
        tvInfo.setText("v1.1|20260612|jazhao");
        tvInfo.setGravity(Gravity.CENTER);
        tvInfo.setTextColor(Color.WHITE);

        LinearLayout topBox = new LinearLayout(this);
        topBox.setOrientation(LinearLayout.VERTICAL);
        topBox.setPadding(20,10,20,10);
        topBox.setBackgroundColor(Color.GRAY);
        topBox.addView(tvStatus);
        topBox.addView(tvInfo);

        TextView btnClose = new TextView(this);
        btnClose.setText("X");
        btnClose.setPadding(20,20,20,20);
        btnClose.setOnClickListener(v -> showExit());

        LinearLayout topBar = new LinearLayout(this);
        topBar.addView(topBox,new LinearLayout.LayoutParams(0,-2,1));
        topBar.addView(btnClose);

        // ✅ 输入框
        etInput = new EditText(this);
        etInput.setHint("Scan or Input");

        etInput.setOnKeyListener((v, keyCode, event) -> {
            if(keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == KeyEvent.ACTION_DOWN){
                process(etInput.getText().toString());
                return true;
            }
            return false;
        });

        // ✅ 扫码按钮（新增）
        Button btnScan = new Button(this);
        btnScan.setText("📷 扫码");

        btnScan.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("对准二维码");
            integrator.setBeepEnabled(false);  // ✅ 不发声音
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        });

        // ✅ 识别结果
        tvExtracted = new TextView(this);
        tvExtracted.setSingleLine(true);
        tvExtracted.setEllipsize(android.text.TextUtils.TruncateAt.END);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        root.addView(topBar);
        root.addView(etInput);
        root.addView(btnScan);   // ✅ 新增按钮
        root.addView(tvExtracted);
        root.addView(container);

        scroll.addView(root);
        setContentView(scroll);
    }

    // ✅ 接收扫码结果（核心）
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult result = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {

            String scanResult = result.getContents();

            etInput.setText(scanResult);   // ✅ 显示
            process(scanResult);           // ✅ 校验
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // ✅ 主逻辑（你原有逻辑基本保留）
    private void process(String input){

        container.removeAllViews();

        if(input == null || input.isEmpty()){
            setStatus(false);
            return;
        }

        String extracted = extract(input);

        // ✅ 去掉换行（关键修复你之前空行问题）
        tvExtracted.setText("识别的效期内容: " + extracted);

        String[] parts = extracted.split("#");

        if(parts.length != 3){
            setStatus(false);
            addBlock("Format Error",extracted,"-","3 parts",false,"Separator error");
            return;
        }

        String prefix = parts[0];

        String id = prefix.substring(0,1);
        String sku = prefix.substring(1,10);
        String batch = prefix.substring(10);

        String pdStr = parts[1];
        String ddStr = parts[2];

        Date today = new Date();

        boolean idOK = id.equals("0");
        boolean skuOK = sku.matches("\\d{9}");
        boolean batchOK = batch.matches("[A-Za-z0-9]{1,15}");

        boolean pdOK = false;
        String pdErr = "";
        Date pd = null;

        if(!pdStr.matches("^\\d{8}$")){
            pdErr = "Must 8 digits";
        }else{
            pd = strictDate(pdStr);
            if(pd == null) pdErr = "Invalid date";
            else if(pd.after(today)) pdErr = "Future";
            else pdOK = true;
        }

        Date dd = null;
        boolean ddOK = true;
        StringBuilder ddErr = new StringBuilder();

        if(!ddStr.matches("^\\d{8}$")){
            ddOK = false;
            ddErr.append("- Must 8 digits\n");
        }else{
            dd = strictDate(ddStr);
            if(dd == null){
                ddOK = false;
                ddErr.append("- Invalid date\n");
            }else if(!dd.after(today)){
                ddOK = false;
                ddErr.append("- Must future\n");
            }
        }

        if(pd != null && dd != null && !pd.before(dd)){
            ddOK = false;
            ddErr.append("- PD < DD\n");
        }

        boolean allOK = idOK && skuOK && batchOK && pdOK && ddOK;

        setStatus(allOK);

        addBlock("ID",id,"1","Must 0",idOK,"");
        addDivider();

        addBlock("SKU",sku,String.valueOf(sku.length()),"9 digits",skuOK,"");
        addDivider();

        addBlock("Batch",batch,String.valueOf(batch.length()),"<=15",batchOK,"");
        addDivider();

        addBlock("PD",pdStr,String.valueOf(pdStr.length()),"<= Today",pdOK,pdErr);
        addDivider();

        addBlock("DD",ddStr,String.valueOf(ddStr.length()),"> Today AND PD<DD",ddOK,ddErr.toString());
    }

    private void setStatus(boolean pass){

        LinearLayout parent=(LinearLayout) tvStatus.getParent();

        if(pass){
            passCount++;
            tvStatus.setText("PASS");
            parent.setBackgroundColor(Color.GREEN);
        }else{
            failCount++;
            tvStatus.setText("FAIL");
            parent.setBackgroundColor(Color.RED);
        }

        tvInfo.setText("v1.0 | P:"+passCount+" F:"+failCount);
    }

    private void addBlock(String name,String value,String len,String rule,boolean pass,String err){
        TextView tv=new TextView(this);
        tv.setPadding(20,6,20,6); // ✅ 已压缩
        tv.setText(
                "[" + name + "]\n" +
                        "二维码内容：" + value + "\n" +
                        "长度：" + len + "\n" +
                        "校验规则：" + rule + "\n" +
                        "结果：" + (pass ? "PASS" : "FAIL") +
                        (err == null || err.isEmpty() ? "" : "\n错误：" + err)
        );
        tv.setBackgroundColor(pass?Color.parseColor("#C8E6C9"):Color.parseColor("#FFCDD2"));
        container.addView(tv);
    }

    private void addDivider(){
        TextView tv=new TextView(this);
        tv.setText("----------------------------------------");
        tv.setGravity(Gravity.CENTER);
        container.addView(tv);
    }

    private void showExit(){
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Quit?")
                .setPositiveButton("Yes",(d,w)->finish())
                .setNegativeButton("No",null)
                .show();
    }

    private String extract(String input){
        if(input.contains("cii1/")){
            String raw=input.substring(input.indexOf("cii1/")+5);
            return raw.replace("&","#");
        }
        return input.replace("&","#");
    }

    private Date strictDate(String s){
        try{
            SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
            sdf.setLenient(false);
            return sdf.parse(s);
        }catch(Exception e){
            return null;
        }
    }
}
