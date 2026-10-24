package com.example.medicinereminder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.TextView;
import java.util.Locale;
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Button btnAddMedicine =findViewById(R.id.btnAddMedicine);
        btnAddMedicine.setOnClickListener(V->{
            Intent intent=new Intent(
                    MainActivity.this,
                    AddMedicineActivity.class
            );
            startActivityForResult(intent,100);//启动新页面并且把数据传回当前页面
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }//适配系统栏（状态栏、导航栏）的边距，防止页面内容被手机顶部状态栏、底部系统导航条挡住。
    protected void onActivityResult(int requestCode,int resultcode,Intent data){
        super.onActivityResult(requestCode,resultcode,data);
        if(requestCode==100&&resultcode==RESULT_OK&&data!=null){
            String name = data.getStringExtra("name");
            String dose = data.getStringExtra("dose");
            String meal = data.getStringExtra("meal");

            int hour = data.getIntExtra("hour", -1);
            int minute = data.getIntExtra("minute", -1);

            boolean repeatDaily =
                    data.getBooleanExtra("repeat_daily", false);

            String time = String.format(
                    Locale.getDefault(),//获取手机系统默认的语言和对象
                    "%02d:%02d",
                    hour,
                    minute
            );

            String repeatText = repeatDaily ? "每天重复" : "仅一次";

            TextView tvEmpty = findViewById(R.id.tvEmpty);

            tvEmpty.setText(
                    "药品名称：" + name + "\n"
                            + "每次用量：" + dose + "\n"
                            + "服用备注：" + meal + "\n"
                            + "提醒时间：" + time + "\n"
                            + "重复方式：" + repeatText
            );
        }
    }
}
