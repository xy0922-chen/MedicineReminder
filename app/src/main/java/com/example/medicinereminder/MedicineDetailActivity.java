package com.example.medicinereminder;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MedicineDetailActivity extends AppCompatActivity {

    private TextView tvMedicineDetail;
    private Button btnDeleteMedicine;

    private MedicineDbHelper dbHelper;
    private long medicineId;

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicine_detail);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, windowInsets) -> {
                    Insets bars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );

                    return windowInsets;
                }
        );

        tvMedicineDetail = findViewById(R.id.tvMedicineDetail);
        btnDeleteMedicine = findViewById(R.id.btnDeleteMedicine);
        Button btnBack = findViewById(R.id.btnBack);

        dbHelper = new MedicineDbHelper(this);

        // 获取首页传来的药品编号。
        medicineId = getIntent().getLongExtra("medicine_id", -1);

        if (medicineId == -1) {
            Toast.makeText(
                    this,
                    "没有获取到药品编号",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }
        loadMedicineDetail();

        btnBack.setOnClickListener(v -> finish());

        btnDeleteMedicine.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除药品")
                    .setMessage("确定删除这条用药计划吗？删除后无法恢复。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton(
                            "删除",
                            (dialog, which) -> deleteMedicine()
                    )
                    .show();
        });


    }

    private void loadMedicineDetail() {
        databaseExecutor.execute(() -> {
            try (Cursor cursor =
                         dbHelper.getMedicineById(medicineId)) {

                if (!cursor.moveToFirst()) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Toast.makeText(
                                this,
                                "这条药品记录已不存在",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();
                    });

                    return;
                }

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                );

                String dose = cursor.getString(
                        cursor.getColumnIndexOrThrow("dose")
                );

                String meal = cursor.getString(
                        cursor.getColumnIndexOrThrow("meal")
                );

                int hour = cursor.getInt(
                        cursor.getColumnIndexOrThrow("hour")
                );

                int minute = cursor.getInt(
                        cursor.getColumnIndexOrThrow("minute")
                );

                boolean repeatDaily = cursor.getInt(
                        cursor.getColumnIndexOrThrow("repeat_daily")
                ) == 1;

                String time = String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hour,
                        minute
                );

                String detail =
                        "药品名称：" + name + "\n\n"
                                + "每次用量：" + dose + "\n\n"
                                + "服用备注：" + meal + "\n\n"
                                + "提醒时间：" + time + "\n\n"
                                + "重复方式："
                                + (repeatDaily ? "每天重复" : "仅一次");

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }//判断当前 Activity 是不是快要关闭 / 已经销毁了

                    tvMedicineDetail.setText(detail);
                    btnDeleteMedicine.setEnabled(true);
                });

            } catch (Exception e) {
                Log.e("MedicineDetail", "读取详情失败", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    //异步回调更新 UI 前，增加 `isFinishing/isDestroyed` 生命周期判断，防止页面销毁后操作 View 引发崩溃

                    tvMedicineDetail.setText("读取失败，请返回首页后重试。");
                });
            }
        });
    }

    private void deleteMedicine() {
        btnDeleteMedicine.setEnabled(false);

        databaseExecutor.execute(() -> {
            try {
                int rows = dbHelper.deleteMedicine(medicineId);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    String message = rows > 0
                            ? "药品已删除"
                            : "这条药品记录已不存在";

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                });

            } catch (Exception e) {
                Log.e("MedicineDetail", "删除失败", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnDeleteMedicine.setEnabled(true);

                    Toast.makeText(
                            this,
                            "删除失败，请重试",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        databaseExecutor.execute(() -> {
            if (dbHelper != null) {
                dbHelper.close();
            }
        });

        databaseExecutor.shutdown();
        super.onDestroy();
    }
}