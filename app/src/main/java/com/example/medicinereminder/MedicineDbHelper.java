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
 public ArrayList<String> getAllMedicines(ArrayList<Long> ids) {
     ArrayList<String> list = new ArrayList<>();
     ids.clear();

     SQLiteDatabase db = getReadableDatabase();

     Cursor cursor = db.query(
             TABLE_MEDICINE,
             new String[]{
                     "_id",
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
             long medicineId = cursor.getLong(
                     cursor.getColumnIndexOrThrow("_id")
             );

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

             String medicineInfo =
                     "药品名称：" + name + "\n"
                             + "每次用量：" + dose + "\n"
                             + "服用备注：" + meal + "\n"
                             + "提醒时间：" + time + "\n"
                             + "重复方式："
                             + (repeatDaily ? "每天重复" : "仅一次");

             // 同一条记录的编号和文字，必须一起加入。
             ids.add(medicineId);
             list.add(medicineInfo);
         }
     } finally {
         cursor.close();
     }

     return list;
 }
    // 根据编号查询一条药品。
    // 调用者读取完成后，需要关闭返回的 Cursor。
    public Cursor getMedicineById(long id) {
        SQLiteDatabase db = getReadableDatabase();

        return db.query(
                TABLE_MEDICINE,
                null,
                "_id = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );
    }

    // 根据编号删除药品，返回删除的行数。
    public int deleteMedicine(long id) {
        SQLiteDatabase db = getWritableDatabase();

        return db.delete(
                TABLE_MEDICINE,
                "_id = ?",
                new String[]{String.valueOf(id)}
        );
    }
}