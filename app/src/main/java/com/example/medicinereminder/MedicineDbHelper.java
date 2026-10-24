package com.example.medicinereminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Locale;

public class MedicineDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "medicine.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_MEDICINE = "medicine";

    public MedicineDbHelper(Context context) {
        super(
                context.getApplicationContext(),
                DATABASE_NAME,
                null,
                DATABASE_VERSION
        );
    }

    @Override
    //是数据库首次创建时调用的方法，不是 Activity 的 onCreate()，也不是每次进入页面都会执行。
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_MEDICINE + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "dose TEXT NOT NULL, "
                + "meal TEXT NOT NULL, "
                + "hour INTEGER NOT NULL, "
                + "minute INTEGER NOT NULL, "
                + "repeat_daily INTEGER NOT NULL DEFAULT 0"
                + ")";

        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion
    ) {
        // 当前只有版本 1。
        // 以后修改表结构并提高版本号时，在这里编写迁移代码。
    }

    //增操作
    public long insertMedicine(
            String name,
            String dose,
            String meal,
            int hour,
            int minute,
            boolean repeatDaily
    ) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("dose", dose);
        values.put("meal", meal);
        values.put("hour", hour);
        values.put("minute", minute);
        values.put("repeat_daily", repeatDaily ? 1 : 0);

        return db.insert(TABLE_MEDICINE, null, values);
    }
 //新增查询方法
 public ArrayList<String> getAllMedicines(){
        ArrayList<String> list=new ArrayList<>();
        SQLiteDatabase db=getReadableDatabase();

        //Cursor（游标）是 SQLite 查询结果的数据集容器，相当于一张结果表格，用来存放 db.query () 查出来的多行数据。
         Cursor cursor = db.query(
                 TABLE_MEDICINE,
                 new String[]{
                         "name",
                         "dose",
                         "meal",
                         "hour",
                         "minute",
                         "repeat_daily"
                 },
                 null,
                 null,
                 null,
                 null,
                 "_id ASC"
         );
     try {
         while (cursor.moveToNext()) {
             String name = cursor.getString(0);
             String dose = cursor.getString(1);
             String meal = cursor.getString(2);

             int hour = cursor.getInt(3);
             int minute = cursor.getInt(4);
             boolean repeatDaily = cursor.getInt(5) == 1;

             String time = String.format(
                     Locale.getDefault(),
                     "%02d:%02d",
                     hour,
                     minute
             );

             String repeatText = repeatDaily ? "每天重复" : "仅一次";

             String medicineInfo =
                     "药品名称：" + name + "\n"
                             + "每次用量：" + dose + "\n"
                             + "服用备注：" + meal + "\n"
                             + "提醒时间：" + time + "\n"
                             + "重复方式：" + repeatText;

             list.add(medicineInfo);
         }
     } finally {
         cursor.close();
     }

     return list;
 }
}