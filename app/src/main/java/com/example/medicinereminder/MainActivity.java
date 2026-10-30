package com.example.medicinereminder;

import android.content.Intent;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private TextView tvTodayOverview;
    private BottomNavigationView bottomNavigation;
    private int selectedTab = R.id.navHome;
    private OnBackPressedCallback tabBackCallback;

    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();
    private ArrayList<Long> medicineIds=new ArrayList<>();

    //除了保存意外，重新打开首页也需要读取
    private void loadMedicines() {
        databaseExecutor.execute(() -> {
            try {
                ArrayList<Long> savedIds = new ArrayList<>();

                ArrayList<String> savedList = dbHelper.getAllMedicines(savedIds);
                int[] overview = dbHelper.getTodayOverview();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    medicineList.clear();
                    medicineList.addAll(savedList);

                    medicineIds.clear();
                    medicineIds.addAll(savedIds);
                    Log.d(
                            "MedicineList",
                            "文字数量=" + medicineList.size()
                                    + "，编号数量=" + medicineIds.size()
                    );
                    medicineAdapter.notifyDataSetChanged();
                    tvTodayOverview.setText(getString(R.string.overview_counts,
                            overview[0], overview[1], overview[2]));
                });

            } catch (Exception e) {
                Log.e("MedicineDatabase", "读取药品失败", e);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    tvTodayOverview.setText(R.string.overview_error);
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
        tvTodayOverview = findViewById(R.id.tvTodayOverview);
        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, MedicineHistoryActivity.class)));
        findViewById(R.id.btnReminderSettings).setOnClickListener(v ->
                startActivity(new Intent(this, ReminderSettingsActivity.class)));
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
       /* if(savedInstanceState!=null){
            ArrayList<String> savedList=savedInstanceState.getStringArrayList("medicine_list");
            if(savedList!=null){
                medicineList=savedList;
            }
        }*/
        medicineAdapter=new ArrayAdapter<>(
                this,
                R.layout.item_medicine,
                medicineList
        );//this:当前页面，上下文 R.layout.item_medcine:每条记录使用的布局 medicineList：要显示的数据集合
        lvMedicine.setAdapter(medicineAdapter);
        lvMedicine.setEmptyView(tvEmpty);
        lvMedicine.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position < 0
                            || position >= medicineIds.size()
                            || medicineIds.size() != medicineList.size()) {

                        Log.e(
                                "MedicineClick",
                                "列表与编号不一致：position=" + position
                                        + ", 文字数量=" + medicineList.size()
                                        + ", 编号数量=" + medicineIds.size()
                        );

                        Toast.makeText(
                                MainActivity.this,
                                "列表正在刷新，请稍后再试",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadMedicines();
                        return;
                    }

                    long medicineId = medicineIds.get(position);

                    Intent intent = new Intent(
                            MainActivity.this,
                            MedicineDetailActivity.class
                    );

                    intent.putExtra("medicine_id", medicineId);

                    startActivity(intent);
                }
        );
        setupBottomNavigation(savedInstanceState);
    }

    // 以下只调整页面显示和导航；原有列表、保存和数据回传方法不变。
    private void setupBottomNavigation(Bundle savedState) {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        MedicineBottomNavigation.style(bottomNavigation);
        tabBackCallback = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                bottomNavigation.setSelectedItemId(R.id.navHome);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, tabBackCallback);
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navRecords) {
                startActivity(new Intent(this, MedicineHistoryActivity.class));
                // 历史页有自己的导航，返回此页时恢复先前的首页/药物选项。
                return false;
            }
            showMainTab(item.getItemId());
            return true;
        });
        int tab = savedState == null
                ? getIntent().getIntExtra(MedicineBottomNavigation.EXTRA_TAB, R.id.navHome)
                : savedState.getInt(MedicineBottomNavigation.EXTRA_TAB, R.id.navHome);
        bottomNavigation.setSelectedItemId(tab == R.id.navMedicines ? R.id.navMedicines : R.id.navHome);
        findViewById(R.id.btnOpenMedicines).setOnClickListener(v ->
                bottomNavigation.setSelectedItemId(R.id.navMedicines));
        findViewById(R.id.btnHomeAddMedicine).setOnClickListener(v ->
                findViewById(R.id.btnAddMedicine).performClick());
    }

    private void showMainTab(int tab) {
        selectedTab = tab == R.id.navMedicines ? R.id.navMedicines : R.id.navHome;
        findViewById(R.id.pageHome).setVisibility(selectedTab == R.id.navHome ? View.VISIBLE : View.GONE);
        findViewById(R.id.pageMedicines).setVisibility(selectedTab == R.id.navMedicines ? View.VISIBLE : View.GONE);
        tabBackCallback.setEnabled(selectedTab != R.id.navHome);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int tab = intent.getIntExtra(MedicineBottomNavigation.EXTRA_TAB, R.id.navHome);
        bottomNavigation.setSelectedItemId(tab == R.id.navMedicines ? R.id.navMedicines : R.id.navHome);
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MedicineBottomNavigation.EXTRA_TAB, selectedTab);
    }

  /*  protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList("medicine_list", medicineList);
    }*/
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
                ArrayList<Long> savedIds=new ArrayList<>();
                ArrayList<String> savedList =
                        dbHelper.getAllMedicines(savedIds);
                int[] overview = dbHelper.getTodayOverview();

                // 第三步：回到主线程更新首页。
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    medicineList.clear();
                    medicineList.addAll(savedList);

                    medicineIds.clear();
                    medicineIds.addAll(savedIds);

                    medicineAdapter.notifyDataSetChanged();
                    tvTodayOverview.setText(getString(R.string.overview_counts,
                            overview[0], overview[1], overview[2]));

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
    @Override
    protected void onResume() {
        super.onResume();

        loadMedicines();
        databaseExecutor.execute(() -> {
            try { ReminderManager.reconcile(getApplicationContext()); }
            catch (Exception e) { Log.e("MedicineReminder", "恢复提醒失败", e); }
        });
    }
}
