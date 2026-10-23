package com.example.medicinereminder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.app.TimePickerDialog;

import java.util.Calendar;
import java.util.Locale;

public class AddMedicineActivity extends AppCompatActivity {

    private EditText etMedicineName;
    private EditText etDose;
    private RadioGroup rgMeal;
    private CheckBox cbDaily;
    private TextView tvPreview;
    private TextView tvReminderTime;
    private int reminderHour=-1;
    private int reminderMinute=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_medicine);

        // 留出系统状态栏和导航栏的位置，避免遮挡界面。
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

        // 找到 XML 中的控件。
        etMedicineName = findViewById(R.id.etMedicineName);
        etDose = findViewById(R.id.etDose);
        rgMeal = findViewById(R.id.rgMeal);
        cbDaily = findViewById(R.id.cbDaily);
        tvPreview = findViewById(R.id.tvPreview);

        Button btnCheck = findViewById(R.id.btnCheck);

        // 点击按钮时，检查并展示输入。
        btnCheck.setOnClickListener(v -> checkMedicineInfo());
    }

    private void checkMedicineInfo() {
        // 先清除上一次的预览，避免显示旧内容。
        tvPreview.setVisibility(View.GONE);

        String name = etMedicineName.getText().toString().trim();
        String dose = etDose.getText().toString().trim();

        if (name.isEmpty()) {
            etMedicineName.setError("请输入药品名称");
            etMedicineName.requestFocus();
            return;
        }

        if (dose.isEmpty()) {
            etDose.setError("请输入每次用量和单位");
            etDose.requestFocus();
            return;
        }

        int checkedId = rgMeal.getCheckedRadioButtonId();

        if (checkedId == -1) {
            Toast.makeText(
                    this,
                    "请选择服用备注",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        RadioButton selectedButton = findViewById(checkedId);
        String meal = selectedButton.getText().toString();

        boolean repeatDaily = cbDaily.isChecked();
        String repeatText = repeatDaily ? "每天重复" : "不重复";

        String preview = "请核对以下信息：\n\n"
                + "药品名称：" + name + "\n"
                + "每次用量：" + dose + "\n"
                + "服用备注：" + meal + "\n"
                + "提醒重复：" + repeatText + "\n\n"
                + "当前仅预览，尚未保存或设置提醒。";

        tvPreview.setText(preview);
        tvPreview.setVisibility(View.VISIBLE);
    }
}