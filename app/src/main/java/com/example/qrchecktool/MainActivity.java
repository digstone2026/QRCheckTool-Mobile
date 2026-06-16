package com.example.qrchecktool;
import com.journeyapps.barcodescanner.IntentIntegrator;
import com.journeyapps.barcodescanner.IntentResult;
// ... (原有的 import 保持不变) ...
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    EditText etInput;
    TextView tvStatus, tvInfo, tvExtracted;
    LinearLayout container;
    int passCount = 0;
    int failCount = 0;
    String lastCode = "";
    long lastScanTime = 0;

    // >>> 1. 新增：相机相关成员变量 <<<
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private boolean isScanning = false; // 防止重复弹窗

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ✅ 摄像头权限（鸿蒙/安卓都需要）
if (checkSelfPermission(android.Manifest.permission.CAMERA)
        != android.content.pm.PackageManager.PERMISSION_GRANTED) {

    requestPermissions(
            new String[]{android.Manifest.permission.CAMERA},
            100
    );
}
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);

        // ... (保持原有的 UI 初始化代码不变) ...
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
        topBox.setPadding(20, 10, 20, 10);
        topBox.setBackgroundColor(Color.GRAY);
        topBox.addView(tvStatus);
        topBox.addView(tvInfo);

        TextView btnClose = new TextView(this);
        btnClose.setText("X");
        btnClose.setPadding(20, 20, 20, 20);
        btnClose.setOnClickListener(v -> showExit());

        LinearLayout topBar = new LinearLayout(this);
        topBar.addView(topBox, new LinearLayout.LayoutParams(0, -2, 1));
        topBar.addView(btnClose);
        root.addView(topBar);

        // --- 修改点：初始化输入框 ---
        etInput = new EditText(this);
        etInput.setHint("点击此处扫码或输入");
        
        // 关键修改：设置为不可聚焦（防止弹出软键盘），但可点击
        etInput.setFocusable(false); 
        etInput.setClickable(true);

        // 设置点击监听器，用于唤起扫码
        etInput.setOnClickListener(v -> {
            if (!isScanning) {
                checkCameraPermission();
            }
        });

        // 保持原有的回车监听
        etInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                process(etInput.getText().toString());
                return true;
            }
            return false;
        });

        // ... (保持原有的 TextWatcher 逻辑) ...
        etInput.addTextChangedListener(new android.text.TextWatcher() {
            long last = 0;
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                long now = System.currentTimeMillis();
                if (now - last < 500) return;
                last = now;
                if (s.length() > 5) {
                    process(s.toString());
                }
            }
        });

        root.addView(etInput);

        // ... (保持原有的 Example Image 逻辑) ...
        ImageView imgExample = new ImageView(this);
        imgExample.setImageResource(R.drawable.example);
        imgExample.setAdjustViewBounds(true);
        imgExample.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imgExample.setPadding(10, 10, 10, 10);
        root.addView(imgExample);

        tvExtracted = new TextView(this);
        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        root.addView(tvExtracted);
        root.addView(container);
        scroll.addView(root);
        setContentView(scroll);

        // 2. 初始化 Camera Executor
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
    }

    // >>> 3. 新增：权限检查逻辑 <<<
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private void checkCameraPermission() {
        if (allPermissionsGranted()) {
            openCameraPreview();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // >>> 4. 新增：打开相机预览并扫码 <<<
    private void openCameraPreview() {
        isScanning = true;
        // 创建一个全屏的 Dialog 来显示相机预览
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(new View(this)) // 临时占位
                .setCancelable(true)
                .show();

        // 调整 Dialog 大小
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // 初始化 PreviewView
        PreviewView viewFinder = new PreviewView(this);
        dialog.setContentView(viewFinder);

        // 初始化 ML Kit 扫码器
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE) // 只识别 QR Code
                .build();
        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        // 绑定生命周期和分析器
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, scanner, viewFinder, dialog);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("QRScanner", "Camera init failed", e);
                isScanning = false;
                dialog.dismiss();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // >>> 5. 新增：绑定预览和分析 <<<
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider, BarcodeScanner scanner, PreviewView viewFinder, AlertDialog dialog) {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new android.util.Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        // 设置分析监听器
        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
            android.media.Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.getImageInfo().getRotationDegrees()
                );

                // 使用 ML Kit 处理图像
                scanner.process(image)
                        .addOnSuccessListener(barcodes -> {
                            if (!barcodes.isEmpty()) {
                                String rawValue = barcodes.get(0).getRawValue();
                                if (rawValue != null && !rawValue.isEmpty()) {
                                    // 在主线程更新 UI
                                    runOnUiThread(() -> {
                                        etInput.setText(rawValue);
                                        process(rawValue); // 自动触发校验
                                        dialog.dismiss(); // 关闭扫码框
                                        isScanning = false;
                                    });
                                }
                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close()) // 必须关闭
                        .addOnFailureListener(e -> {
                            imageProxy.close();
                            Log.e("MLKit", "Scan failed", e);
                        });
            } else {
                imageProxy.close();
            }
        });

        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e("CameraX", "Use case binding failed", e);
            isScanning = false;
            dialog.dismiss();
        }
    }

    // --- 保持原有的业务逻辑方法不变 ---
    // (包括 getScanData, canScan, receiver, setStatus, process, extract, strictDate, showExit)
    // ... (此处保留你上传的文件中对应的方法代码) ...

    // --- 原有方法省略 (请保留你文件里的这些方法) ---
    // 请确保保留以下方法：
    // private String getScanData(Intent intent) { ... }
    // private boolean canScan() { ... }
    // private BroadcastReceiver receiver = ... 
    // @Override protected void onResume() { ... }
    // @Override protected void onPause() { ... }
    // private void setStatus(boolean pass) { ... }
    // private void addBlock(...) { ... }
    // private void addDivider() { ... }
    // private void showExit() { ... }
    // private String extract(String input) { ... }
    // private Date strictDate(String s) { ... }
}
