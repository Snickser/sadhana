package ru.rolyc.sadhana;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Главный экран: список отчётов + экспорт/импорт файла. */
public class MainActivity extends Activity {

    private static final int REQ_EXPORT = 1001;
    private static final int REQ_IMPORT = 1002;
    private static final int REQ_NOTIFY_PERMISSION = 1003;
    private static final int MAX_IMPORT_CHARS = 20 * 1024 * 1024;

    private ReportsDb db;
    private ReportAdapter adapter;
    private ListView list;
    private TextView empty;
    private TextView summary;

    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat human = new SimpleDateFormat("d MMMM yyyy", new Locale("ru", "RU"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new ReportsDb(this);
        list = (ListView) findViewById(R.id.list);
        empty = (TextView) findViewById(R.id.empty);
        summary = (TextView) findViewById(R.id.summary);

        adapter = new ReportAdapter(this);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openEditor(adapter.getItem(position).date);
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDelete(adapter.getItem(position));
                return true;
            }
        });

        Button add = (Button) findViewById(R.id.btnAdd);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditor(today());
            }
        });

        Button export = (Button) findViewById(R.id.btnExport);
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExport();
            }
        });

        Button importBtn = (Button) findViewById(R.id.btnImport);
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImport();
            }
        });

        Button settings = (Button) findViewById(R.id.btnSettings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReminderDialog();
            }
        });

        Button deleteAll = (Button) findViewById(R.id.btnDeleteAll);
        deleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteAll();
            }
        });

        Button donate = (Button) findViewById(R.id.btnDonate);
        donate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDonatePage();
            }
        });

        Button stats = (Button) findViewById(R.id.btnStats);
        stats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, StatsActivity.class));
            }
        });

        ReminderScheduler.apply(this);
        Notifications.ensureChannel(this);

        refresh();
    }

    private void confirmDeleteAll() {
        final int total = db.count();
        if (total == 0) {
            Toast.makeText(this, "База уже пуста", Toast.LENGTH_SHORT).show();
            return;
        }
        String word = total == 1 ? "отчёт" : (total < 5 ? "отчёта" : "отчётов");
        new AlertDialog.Builder(this)
                .setTitle("Удалить всё?")
                .setMessage("Будет удалено " + total + " " + word + " — вся база целиком.\n\n"
                        + "Действие необратимо: восстановить можно только из файла копии. "
                        + "Если копии нет, сделайте её кнопкой «Копия».")
                .setNeutralButton("Копия", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startExport();
                    }
                })
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить всё", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int deleted = db.deleteAll();
                        refresh();
                        Toast.makeText(MainActivity.this,
                                "Удалено записей: " + deleted, Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    // ---------------- донат ----------------

    /** Страница сбора средств. Открывается в браузере, ничего не отправляет из приложения. */
    public static final String DONATE_URL = "https://yoomoney.ru/fundraise/1HM6IRLA8V7.260510";

    private void openDonatePage() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL));
        try {
            startActivity(i);
        } catch (Exception e) {
            // браузера нет или ссылку некому открыть — показываем адрес, чтобы скопировать
            new AlertDialog.Builder(this)
                    .setTitle("Поддержать проект")
                    .setMessage("Не удалось открыть браузер.\n\nАдрес страницы:\n" + DONATE_URL)
                    .setPositiveButton("ОК", null)
                    .show();
        }
    }

    // ---------------- напоминание ----------------

    private void showReminderDialog() {
        final View content = getLayoutInflater().inflate(R.layout.dialog_reminder, null);
        final CheckBox enabled = (CheckBox) content.findViewById(R.id.cbReminderEnabled);
        final Button timeBtn = (Button) content.findViewById(R.id.btnReminderTime);
        final TextView hint = (TextView) content.findViewById(R.id.tvReminderHint);

        final int[] time = {ReminderPrefs.hour(this), ReminderPrefs.minute(this)};
        enabled.setChecked(ReminderPrefs.enabled(this));
        timeBtn.setText(ReminderTime.label(time[0], time[1]));
        updateHint(hint, enabled.isChecked(), time[0], time[1]);

        enabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateHint(hint, enabled.isChecked(), time[0], time[1]);
            }
        });

        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        time[0] = hourOfDay;
                        time[1] = minute;
                        timeBtn.setText(ReminderTime.label(time[0], time[1]));
                        updateHint(hint, enabled.isChecked(), time[0], time[1]);
                    }
                }, time[0], time[1], true).show();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("Напоминание")
                .setView(content)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ReminderPrefs.save(MainActivity.this, enabled.isChecked(), time[0], time[1]);
                        ReminderScheduler.apply(MainActivity.this);
                        if (enabled.isChecked()) {
                            askNotificationPermission();
                            toastReminderOn(time[0], time[1]);
                        } else {
                            Notifications.clear(MainActivity.this);
                            Toast.makeText(MainActivity.this, "Напоминание выключено",
                                    Toast.LENGTH_SHORT).show();
                        }
                        refresh();
                    }
                })
                .show();
    }

    private void updateHint(TextView hint, boolean on, int hour, int minute) {
        if (!on) {
            hint.setText("Уведомление приходить не будет.");
            return;
        }
        String label = ReminderTime.label(hour, minute);
        hint.setText("Каждый день в " + label + ". Если отчёт за день уже заполнен, "
                + "уведомление об этом и скажет.");
    }

    private void toastReminderOn(int hour, int minute) {
        Toast.makeText(this, "Напоминание в " + ReminderTime.label(hour, minute) + " каждый день",
                Toast.LENGTH_LONG).show();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_NOTIFY_PERMISSION) return;
        boolean granted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!granted) {
            new AlertDialog.Builder(this)
                    .setTitle("Уведомления запрещены")
                    .setMessage("Напоминание настроено, но Android не покажет уведомление, "
                            + "пока вы не разрешите уведомления для приложения в настройках.")
                    .setPositiveButton("ОК", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Приложение открыли — напоминание больше не висит в шторке.
        Notifications.clear(this);
        refresh();
    }

    // ---------------- список ----------------

    private void refresh() {
        List<Report> reports = db.all();
        adapter.setData(reports);
        empty.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
        summary.setText(summaryLine(reports));
    }

    private String summaryLine(List<Report> reports) {
        if (reports.isEmpty()) {
            return "База пуста. Заполните первый отчёт — данные останутся только на телефоне.";
        }
        Calendar border = Calendar.getInstance();
        border.add(Calendar.DAY_OF_YEAR, -6);
        String borderIso = iso.format(border.getTime());

        int week = 0;
        int weekRounds = 0;
        int weekWake = 0;
        for (int i = 0; i < reports.size(); i++) {
            Report r = reports.get(i);
            if (r.date.compareTo(borderIso) >= 0) {
                week++;
                weekRounds += r.rounds;
                if (r.wakeEarly) weekWake++;
            }
        }
        int avg = week > 0 ? Math.round((float) weekRounds / week) : 0;
        return "Всего отчётов: " + reports.size()
                + " · за 7 дней: " + week
                + " · среднее кругов: " + avg
                + " · подъём до 5:00: " + weekWake + " дн.";
    }

    private void openEditor(String date) {
        Intent i = new Intent(this, EditActivity.class);
        i.putExtra(EditActivity.EXTRA_DATE, date);
        startActivity(i);
    }

    private void confirmDelete(final Report r) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить отчёт?")
                .setMessage(human.format(safeDate(r.date)) + "\nВосстановить можно только из файла резервной копии.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.delete(r.date);
                        refresh();
                        Toast.makeText(MainActivity.this, "Отчёт удалён", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    // ---------------- экспорт ----------------

    private void startExport() {
        if (db.count() == 0) {
            Toast.makeText(this, "Нечего экспортировать — база пуста", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "sadhana-" + today() + ".json");
        try {
            startActivityForResult(i, REQ_EXPORT);
        } catch (Exception e) {
            showError("Нет файлового менеджера для сохранения файла.\n" + e.getMessage());
        }
    }

    private void doExport(Uri uri) {
        List<Report> reports = db.all();
        String json = ReportJson.toJson(reports, System.currentTimeMillis());
        OutputStream out = null;
        try {
            out = getContentResolver().openOutputStream(uri, "wt");
            if (out == null) throw new IOException("провайдер не дал поток записи");
            out.write(json.getBytes("UTF-8"));
            out.flush();
            Toast.makeText(this, "Экспортировано отчётов: " + reports.size(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            showError("Не удалось записать файл: " + e.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    // ---------------- импорт ----------------

    private void startImport() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_IMPORT);
        } catch (Exception e) {
            showError("Нет файлового менеджера для выбора файла.\n" + e.getMessage());
        }
    }

    private void doImport(Uri uri) {
        String text;
        try {
            text = readAll(uri);
        } catch (Exception e) {
            showError("Не удалось прочитать файл: " + e.getMessage());
            return;
        }
        List<Report> imported;
        try {
            imported = ReportJson.fromJson(text);
        } catch (IllegalArgumentException e) {
            showError("Файл не распознан.\n" + e.getMessage());
            return;
        } catch (RuntimeException e) {
            showError("Файл повреждён: " + e.getMessage());
            return;
        }

        int added = 0;
        int updated = 0;
        int skipped = 0;
        for (int i = 0; i < imported.size(); i++) {
            Report r = imported.get(i);
            if (r.updatedAt <= 0) r.updatedAt = System.currentTimeMillis();
            String res = db.importOne(r);
            if ("new".equals(res)) added++;
            else if ("updated".equals(res)) updated++;
            else skipped++;
        }
        refresh();
        new AlertDialog.Builder(this)
                .setTitle("Импорт завершён")
                .setMessage("Добавлено: " + added + "\nОбновлено: " + updated
                        + "\nПропущено (локальная запись свежее): " + skipped
                        + "\n\nВсего в базе: " + db.count())
                .setPositiveButton("ОК", null)
                .show();
    }

    private String readAll(Uri uri) throws IOException {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("провайдер не дал поток чтения");
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        try {
            int n;
            while ((n = br.read(buf)) > 0) {
                sb.append(buf, 0, n);
                if (sb.length() > MAX_IMPORT_CHARS) throw new IOException("файл слишком большой");
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }

    // ---------------- служебное ----------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQ_EXPORT) doExport(uri);
        else if (requestCode == REQ_IMPORT) doImport(uri);
    }

    private String today() {
        return iso.format(new Date());
    }

    private Date safeDate(String isoDate) {
        try {
            Date d = iso.parse(isoDate);
            return d != null ? d : new Date();
        } catch (Exception e) {
            return new Date();
        }
    }

    private void showError(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage(msg)
                .setPositiveButton("ОК", null)
                .show();
    }
}
