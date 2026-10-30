package com.example.medicinereminder;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** 只负责页面导航，不参与药品、提醒或历史记录的数据处理。 */
final class MedicineBottomNavigation {
    static final String EXTRA_TAB = "navigation_tab";

    static void style(BottomNavigationView bar) {
        bar.setItemActiveIndicatorColor(ColorStateList.valueOf(Color.rgb(221, 238, 228)));
    }

    static void bindHistory(AppCompatActivity activity) {
        BottomNavigationView bar = activity.findViewById(R.id.bottomNavigation);
        style(bar);
        bar.setSelectedItemId(R.id.navRecords);
        bar.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navRecords) return true;
            Intent home = new Intent(activity, MainActivity.class)
                    .putExtra(EXTRA_TAB, item.getItemId())
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(home);
            activity.finish();
            return true;
        });
    }
}
