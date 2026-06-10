package com.example.qrchecktool;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    EditText etInput;
    TextView tvStatus, tvInfo, tvExtracted;
    LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // ✅ 状态栏（超大）
        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextSize(42);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setPadding(20,50,20,50);
        tvStatus.setBackgroundColor(Color.GRAY);

        // ✅ 退出按钮
        TextView close = new TextView(this);
        close.setText("❌");
        close.setTextSize(24);
        close.setPadding(20,20,20,20);
        close.setOnClickListener(v -> showExit());

        LinearLayout topBar = new LinearLayout(this);
        topBar.addView(tvStatus,new LinearLayout.LayoutParams(0,-2,1));
        topBar.addView(close);

        // ✅ 版本信息
        tvInfo = new TextView(this);
        tvInfo.setText(
                "Version : v1.0\n" +
                "Update  : 20260610\n" +
                "Author  : jazhao"
        );
        tvInfo.setPadding(20,10,20,10);
        tvInfo.setBackgroundColor(Color.parseColor("#EEEEEE"));

        etInput = new EditText(this);
        etInput.setHint("扫码输入");

        tvExtracted = new TextView(this);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        root.addView(topBar);
        root.addView(tvInfo);
        root.addView(etInput);
        root.addView(tvExtracted);
        root.addView(container);

        scroll.addView(root);
        setContentView(scroll);

        etInput.setOnKeyListener((v, keyCode, event) -> {
            if(keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == KeyEvent.ACTION_DOWN){
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
            addBlock("格式错误", extracted, "-", "必须3段",
                    false, "分隔符错误");
            return;
        }

        String prefix = parts[0];

        String id = prefix.substring(0,1);
        String sku = prefix.substring(1,10);
        String batch = prefix.substring(10);

        String pdStr = parts[1];
        String ddStr = parts[2];

        Date today = new Date();

        // ✅ 基础字段
        boolean idOK = id.equals("0");
        boolean skuOK = sku.matches("\\d{9}");
        boolean batchOK = batch.matches("[A-Za-z0-9]{1,15}");

        // ✅ Production Date
        Date pd = strictDate(pdStr);
        boolean pdOK = false;
        String pdErr = "";

        if(!pdStr.matches("^\\d{8}$")){
            pdErr = "必须8位数字";
        }else if(pd == null){
            pdErr = "非法日期";
        }else if(pd.after(today)){
            pdErr = "不能大于今天";
        }else{
            pdOK = true;
        }

        // ✅ ✅ ✅ Due Date（合并全部逻辑）
        Date dd = null;
        boolean ddOK = true;
        StringBuilder ddErr = new StringBuilder();

        // 格式
        if(!ddStr.matches("^\\d{8}$")){
            ddOK = false;
            ddErr.append("- 必须为8位纯数字（无空格/无字符）\n");
        }else{
            dd = strictDate(ddStr);

            if(dd == null){
                ddOK = false;
                ddErr.append("- 非法日期\n");
            }else if(!dd.after(today)){
                ddOK = false;
                ddErr.append("- 必须大于今天\n");
            }
        }

        // ✅ 关系校验
        if(pd != null && dd != null && !pd.before(dd)){
            ddOK = false;
            ddErr.append("- 生产日期必须小于到期日期\n");
        }

        boolean allOK = idOK && skuOK && batchOK && pdOK && ddOK;

        setStatus(allOK);

        String line = "----------------------------------------";

        addBlock("Identification Number",id,"1","必须为0",
                idOK,idOK?"":"必须为0");
        addDivider(line);

        addBlock("SKU Number",sku,String.valueOf(sku.length()),
                "9位数字",
                skuOK,skuOK?"":"错误");
        addDivider(line);

        addBlock("Batch Number",batch,String.valueOf(batch.length()),
                "≤15位",
                batchOK,batchOK?"":"错误");
        addDivider(line);

        addBlock("Production Date",pdStr,String.valueOf(pdStr.length()),
                "YYYYMMDD ≤ today",
                pdOK,pdOK?"":pdErr);
        addDivider(line);

        addBlock("Due Date",ddStr,String.valueOf(ddStr.length()),
                "YYYYMMDD > today（且无多余字符） AND PD < DD",
                ddOK,
                ddOK?"":ddErr.toString());
    }

    private void addBlock(String name,String value,String len,
                          String rule,boolean pass,String err){

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20,20,20,20);

        box.setBackgroundColor(pass ?
                Color.parseColor("#C8E6C9") :
                Color.parseColor("#FFCDD2"));

        addText(box,"["+name+"]");
        addText(box,"二维码内容："+value);
        addText(box,"长度："+len);
        addText(box,"校验规则："+rule);
        addText(box,"结果："+(pass?"✅ PASS":"❌ FAIL"));

        if(!pass && err!=null && !err.isEmpty()){
            addText(box,"错误：\n"+err);
        }

        container.addView(box);
    }

    private void addText(LinearLayout box,String txt){
        TextView tv = new TextView(this);
        tv.setText(txt);
        box.addView(tv);
    }

    private void addDivider(String line){
        TextView tv = new TextView(this);
        tv.setText(line);
        tv.setGravity(Gravity.CENTER);
        container.addView(tv);
    }

    private void setStatus(boolean pass){
        ToneGenerator t=new ToneGenerator(AudioManager.STREAM_MUSIC,100);

        if(pass){
            tvStatus.setText("✅ PASS");
            tvStatus.setBackgroundColor(Color.parseColor("#00C853"));
            t.startTone(ToneGenerator.TONE_PROP_BEEP);
        }else{
            tvStatus.setText("❌ FAIL");
            tvStatus.setBackgroundColor(Color.parseColor("#D50000"));
            t.startTone(ToneGenerator.TONE_SUP_ERROR);
        }
    }

    private void showExit(){
        new AlertDialog.Builder(this)
                .setTitle("确认退出")
                .setMessage("是否退出？")
                .setPositiveButton("确认",(d,w)->finish())
                .setNegativeButton("取消",null)
                .show();
    }

    private String extract(String input){
        if(input.contains("cii1/")){
            String raw = input.substring(input.indexOf("cii1/")+5);
            return raw.replace("&","#"); // ✅ 不trim
        }
        return input.replace("&","#");
    }

    private Date strictDate(String s){
        if(!s.matches("\\d{8}")) return null;
        try{
            SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
            sdf.setLenient(false);
            return sdf.parse(s);
        }catch(Exception e){
            return null;
        }
    }
}
