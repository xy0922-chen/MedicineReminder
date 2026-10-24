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
import android.widget.ListView;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.Locale;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    //新增listview
    private ListView lvMedicine;//view用于展示
    private ArrayList<String> medicineList=new ArrayList<>();//存放多多条药品文字
    private ArrayAdapter<String> medicineAdapter;//将集合上的数据转换成列表条目
    private MedicineDbHelper dbHelper;

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    //除了保存意外，重新打开首页也需要读取
    private void loadMedicines() {
        databaseExecutor.execute(() -> {
            try {
                ArrayList<String> savedList =
                        dbHelper.getAllMedicines();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    medicineList.clear();
                    medicineList.addAll(savedList);
                    medicineAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e("MedicineDatabase", "读取药品失败", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(
                            MainActivity.this,
                            "读取药品失败",
                            Toast.LENGTH_SHORT
                    ).show();
                });
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        dbHelper=new MedicineDbHelper(this);//初始化一次
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
        });//适配系统栏（状态栏、导航栏）的边距，防止页面内容被手机顶部状态栏、底部系统导航条挡住。


        //listView使用
        lvMedicine=findViewById(R.id.lvMedicine);
        TextView tvEmpty=findViewById(R.id.tvEmpty);
        if(savedInstanceState!=null){
            ArrayList<String> savedList=savedInstanceState.getStringArrayList("medicine_list");
            if(savedList!=null){
                medicineList=savedList;
            }
        }
        medicineAdapter=new ArrayAdapter<>(
                this,
                R.layout.item_medicine,
                medicineList
        );//this:当前页面，上下文 R.layout.item_medcine:每条记录使用的布局 medicineList：要显示的数据集合
        lvMedicine.setAdapter(medicineAdapter);
        lvMedicine.setEmptyView(tvEmpty);
        loadMedicines();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList("medicine_list", medicineList);
    }
    private void saveMedicine(
            String name,
            String dose,
            String meal,
            int hour,
            int minute,
            boolean repeatDaily
    ) {
        databaseExecutor.execute(() -> {
            try {
                // 第一步：把六项数据写入数据库。
                long id = dbHelper.insertMedicine(
                        name,
                        dose,
                        meal,
                        hour,
                        minute,
                        repeatDaily
                );

                if (id == -1) {
                    throw new IllegalStateException("插入药品失败");
                }

                // 第二步：读取保存后的全部记录。
                ArrayList<String> savedList =
                        dbHelper.getAllMedicines();

                // 第三步：回到主线程更新首页。
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    medicineList.clear();
                    medicineList.addAll(savedList);

                    medicineAdapter.notifyDataSetChanged();

                    Toast.makeText(
                            MainActivity.this,
                            "药品已保存",
                            Toast.LENGTH_SHORT
                    ).show();
                });

            } catch (Exception e) {
                Log.e("MedicineDatabase", "保存或刷新失败", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(
                            MainActivity.this,
                            "保存或刷新失败，请重新打开首页检查",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }
    protected void onActivityResult(int requestCode,int resultcode,Intent data) {
        super.onActivityResult(requestCode, resultcode, data);
        if (requestCode == 100 && resultcode == RESULT_OK && data != null) {
            String name = data.getStringExtra("name");
            String dose = data.getStringExtra("dose");
            String meal = data.getStringExtra("meal");

            int hour = data.getIntExtra("hour", -1);
            int minute = data.getIntExtra("minute", -1);

            boolean repeatDaily =
                    data.getBooleanExtra("repeat_daily", false);
/*
            String time = String.format(
                    Locale.getDefault(),//获取手机系统默认的语言和对象
                    "%02d:%02d",
                    hour,
                    minute
            );

            String repeatText = repeatDaily ? "每天重复" : "仅一次";

/*            TextView tvEmpty = findViewById(R.id.tvEmpty);

            tvEmpty.setText(
                    "药品名称：" + name + "\n"
                            + "每次用量：" + dose + "\n"
                            + "服用备注：" + meal + "\n"
                            + "提醒时间：" + time + "\n"
                            + "重复方式：" + repeatText
            );
            String medicineInfo= "药品名称：" + name + "\n"
                    + "每次用量：" + dose + "\n"
                    + "服用备注：" + meal + "\n"
                    + "提醒时间：" + time + "\n"
                    + "重复方式：" + repeatText;
            medicineList.add(medicineInfo);
            medicineAdapter.notifyDataSetChanged();
        }*/
            saveMedicine(
                    name,
                    dose,
                    meal,
                    hour,
                    minute,
                    repeatDaily);
        }
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
    }//页面销毁时关闭资源
}
