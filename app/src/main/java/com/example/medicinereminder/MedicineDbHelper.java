package com.example.medicinereminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
}