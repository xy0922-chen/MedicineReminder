package com.example.medicinereminder;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Calendar;
import static org.junit.Assert.*;

/** 使用内存数据库，不接触用户的 medicine.db。需在模拟器/真机运行。 */
@RunWith(AndroidJUnit4.class)
public class MedicineHistoryStoreTest {
    private SQLiteDatabase db;
    @Before public void setup() {
        db = SQLiteDatabase.create(null);
        db.execSQL("CREATE TABLE medicine (_id INTEGER PRIMARY KEY, name TEXT NOT NULL,"
                + "dose TEXT NOT NULL, meal TEXT NOT NULL, hour INTEGER NOT NULL,"
                + "minute INTEGER NOT NULL, repeat_daily INTEGER DEFAULT 1, last_taken_at INTEGER DEFAULT 0)");
        db.execSQL("INSERT INTO medicine VALUES (1,'原药名','1片','饭后',8,0,1,0)");
        MedicineHistoryStore.create(db);
    }
    @After public void close() { db.close(); }
    private long day(int date) {
        Calendar c = Calendar.getInstance(); c.clear(); c.set(2026, Calendar.OCTOBER, date, 8, 0);
        return c.getTimeInMillis();
    }
    private int count() {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM medicine_history", null)) {
            c.moveToFirst(); return c.getInt(0);
        }
    }
    @Test public void markSameDayIsIdempotentAndOverviewCountsOnce() {
        assertEquals(1, MedicineHistoryStore.mark(db, 1, day(29)));
        MedicineHistoryStore.mark(db, 1, day(29) + 60000);
        assertEquals(1, count());
        assertArrayEquals(new int[]{1, 1, 0}, MedicineHistoryStore.overview(db, day(29)));
        assertArrayEquals(new int[]{1, 0, 1}, MedicineHistoryStore.overview(db, day(30)));
    }
    @Test public void undoTodayRetainsYesterdayAndRestoresLatest() {
        MedicineHistoryStore.mark(db, 1, day(29));
        MedicineHistoryStore.mark(db, 1, day(30));
        MedicineHistoryStore.undoToday(db, 1, day(30));
        assertEquals(1, count());
        try (Cursor c = db.rawQuery("SELECT last_taken_at FROM medicine WHERE _id=1", null)) {
            c.moveToFirst(); assertEquals(day(29), c.getLong(0));
        }
        assertArrayEquals(new int[]{1, 0, 1}, MedicineHistoryStore.overview(db, day(30)));
    }
    @Test public void editAndDeleteDoNotRewriteSnapshots() {
        MedicineHistoryStore.mark(db, 1, day(29));
        db.execSQL("UPDATE medicine SET name='新药名',dose='2片' WHERE _id=1");
        db.delete("medicine", "_id=1", null);
        String history = MedicineHistoryStore.history(db, 0, day(30)).get(0);
        assertTrue(history.contains("原药名")); assertTrue(history.contains("1片"));
        assertFalse(history.contains("新药名"));
        assertArrayEquals(new int[]{0, 0, 0}, MedicineHistoryStore.overview(db, day(30)));
    }
    @Test public void migrationImportsOnlyKnownLatestRecord() {
        db.execSQL("UPDATE medicine SET last_taken_at=" + day(29));
        MedicineHistoryStore.create(db);
        MedicineHistoryStore.create(db);
        assertEquals(1, count());
        assertTrue(MedicineHistoryStore.history(db, 0, day(30)).get(0).contains("旧版最近记录导入"));
    }
    @Test public void failureRollsBackHistoryAndCurrentStatusTogether() {
        db.execSQL("CREATE TRIGGER reject_status BEFORE UPDATE ON medicine "
                + "BEGIN SELECT RAISE(ABORT, 'test rollback'); END");
        try { MedicineHistoryStore.mark(db, 1, day(30)); fail("Expected failure"); }
        catch (android.database.sqlite.SQLiteException expected) { /* transaction rollback */ }
        assertEquals(0, count());
    }
    @Test public void nonExistentPlanDoesNotCreateRecord() {
        assertEquals(0, MedicineHistoryStore.mark(db, 999, day(30)));
        assertEquals(0, count());
    }
    @Test public void staleUndoDoesNotEraseDeletedPlansHistory() {
        MedicineHistoryStore.mark(db, 1, day(30));
        db.delete("medicine", "_id=1", null);
        assertEquals(0, MedicineHistoryStore.undoToday(db, 1, day(30)));
        assertEquals(1, count());
    }
    @Test public void emptyHistoryDoesNotInventDashboardEvents() {
        DashboardData data = DashboardData.read(db, day(30));
        assertArrayEquals(new int[]{1, 0, 1}, data.overview);
        assertArrayEquals(new int[7], data.week);
        assertTrue(data.recent.isEmpty());
        assertEquals("2026-10-24", data.days.get(0));
        assertEquals("2026-10-30", data.days.get(6));
    }
    @Test public void weeklyChartIncludesRetainedHistoryButOverviewOnlyCurrentPlans() {
        MedicineHistoryStore.mark(db, 1, day(23)); // 范围之外
        MedicineHistoryStore.mark(db, 1, day(29));
        MedicineHistoryStore.mark(db, 1, day(30));
        db.delete("medicine", "_id=1", null);
        DashboardData data = DashboardData.read(db, day(30));
        assertArrayEquals(new int[]{0, 0, 0}, data.overview);
        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 1, 1}, data.week);
        assertEquals(3, data.recent.size());
        assertEquals("原药名", data.recent.get(0).name);
        assertTrue(HistoryRecordAdapter.read(db, 1, day(30), 200).size() == 1);
    }
}
