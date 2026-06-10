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

        // ✅ 顶部栏
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);

        tvStatus = new TextView(this);
        tvStatus.setText("READY");
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setBackgroundColor(Color.GRAY);
        tvStatus.setPadding(20,20,20,20);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT,1);
        tvStatus.setLayoutParams(p);

        TextView btnClose = new TextView(this);
        btnClose.setText("❌");
        btnClose.setTextSize(20);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setPadding(20,20,20,20);

        btnClose.setOnClickListener(v -> showExit());

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

    private void showExit(){
        new AlertDialog.Builder(this)
                .setTitle("确认退出")
                .setMessage("是否退出？")
                .setPositiveButton("确认",(d,w)->finish())
                .setNegativeButton("取消",null)
                .show();
    }

    private void process(String input){

        if(input == null || input.isEmpty()){
            setStatus(false);
            return;
        }

        String extracted = extract(input);
        tvExtracted.setText("提取后内容：\n"+extracted+"\n");

        String[] parts = extracted.split("#");
        if(parts.length!=3){
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
        boolean relOK = pd!=null && dd!=null && pd.before(dd);

        boolean allOK = idOK && skuOK && batchOK && pdOK && ddOK && relOK;

        setStatus(allOK);

        String line = "----------------------------------------\n";

        StringBuilder sb = new StringBuilder();

        sb.append(block("Identification Number",id,"1","必须为0",idOK,idOK?"":"必须为0")).append(line);
        sb.append(block("SKU Number",sku,String.valueOf(sku.length()),"9位数字",skuOK,skuOK?"":"错误")).append(line);
        sb.append(block("Batch Number",batch,String.valueOf(batch.length()),"≤15位",batchOK,batchOK?"":"错误")).append(line);
        sb.append(block("Production Date",pdStr,String.valueOf(pdStr.length()),"YYYYMMDD ≤ today",pdOK,
                pd==null?"非法日期":pd.after(today)?"大于今天":"")).append(line);
        sb.append(block("Due Date",ddStr,String.valueOf(ddStr.length()),"YYYYMMDD > today",ddOK,
                dd==null?"非法日期":!dd.after(today)?"必须大于今天":"")).append(line);
        sb.append(block("Date Relation","-","-","PD < DD",relOK,
                relOK?"":"日期顺序错误"));

        tvResult.setText(sb.toString());
    }

    private String block(String name,String value,String len,
                         String rule,boolean pass,String err){

        StringBuilder s=new StringBuilder();
        s.append("[").append(name).append("]\n");
        s.append("二维码内容：").append(value).append("\n");
        s.append("长度：").append(len).append("\n");
        s.append("校验规则：").append(rule).append("\n");
        s.append("校验结果：").append(pass?"TRUE":"FALSE").append("\n");

        if(!pass && err!=null && !err.isEmpty()){
            s.append("错误信息：").append(err).append("\n");
        }

        return s.toString();
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

    private String extract(String input){
        if(input.contains("cii1/")){
            String raw=input.substring(input.indexOf("cii1/")+5);
            return raw.replace("&","#").trim();
        }
        return input.replace("&","#").trim();
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
