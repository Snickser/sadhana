package ru.rolyc.sadhana;

import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/**
 * Отступ под системную навигационную панель.
 *
 * Приложение рисуется от края до края, поэтому панель жестов (или кнопки навигации)
 * ложится поверх низа экрана и перекрывает нижние кнопки. Здесь высота панели
 * добавляется к нижнему отступу панели кнопок.
 */
public final class NavInsets {

    private NavInsets() {
    }

    public static void applyBottomInset(final View view) {
        if (view == null || Build.VERSION.SDK_INT < 20) return;

        final int basePadding = view.getPaddingBottom();

        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int bottom = bottomInset(insets);
                int wanted = basePadding + bottom;
                if (v.getPaddingBottom() != wanted) {
                    v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                            v.getPaddingRight(), wanted);
                }
                return insets;
            }
        });
        view.requestApplyInsets();
    }

    @SuppressWarnings("deprecation")
    private static int bottomInset(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= 30) {
            return insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
        }
        if (Build.VERSION.SDK_INT >= 20) {
            return insets.getSystemWindowInsetBottom();
        }
        return 0;
    }
}
