package ru.rolyc.sadhana;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Заполнение отчёта за один день. */
public class EditActivity extends Activity {

    public static final String EXTRA_DATE = "date";

    private ReportsDb db;
    private String date;
    private String originalDate;

    private TextView dateView;
    private EditText inRounds;
    private EditText inReading;
    private EditText inService;
    private EditText inNotes;
    private CheckBox cbWake;
    private CheckBox cbSleep;
    private CheckBox cbArati;
    private CheckBox cbLecture;
    private CheckBox cbPrasadam;
    private Button btnDelete;

    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat human = new SimpleDateFormat("d MMMM yyyy, EEEE", new Locale("ru", "RU"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        db = new ReportsDb(this);
        String extra = getIntent().getStringExtra(EXTRA_DATE);
        date = (extra != null && ReportJson.isIsoDate(extra)) ? extra : iso.format(new Date());
        originalDate = date;

        dateView = (TextView) findViewById(R.id.editDate);
        inRounds = (EditText) findViewById(R.id.inRounds);
        inReading = (EditText) findViewById(R.id.inReading);
        inService = (EditText) findViewById(R.id.inService);
        inNotes = (EditText) findViewById(R.id.inNotes);
        cbWake = (CheckBox) findViewById(R.id.cbWake);
        cbSleep = (CheckBox) findViewById(R.id.cbSleep);
        cbArati = (CheckBox) findViewById(R.id.cbArati);
        cbLecture = (CheckBox) findViewById(R.id.cbLecture);
        cbPrasadam = (CheckBox) findViewById(R.id.cbPrasadam);
        btnDelete = (Button) findViewById(R.id.btnDelete);

        Button pick = (Button) findViewById(R.id.btnPickDate);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate();
            }
        });

        Button back = (Button) findViewById(R.id.btnBack);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button save = (Button) findViewById(R.id.btnSave);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });

        wireRoundsButtons();

        load();
    }

    /** Кнопки вокруг поля кругов: работают даже если клавиатура ввела что-то странное. */
    private void wireRoundsButtons() {
        Button minus = (Button) findViewById(R.id.btnRoundsMinus);
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepRounds(-1);
            }
        });

        Button plus = (Button) findViewById(R.id.btnRoundsPlus);
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepRounds(1);
            }
        });

        int[] ids = {R.id.btnR4, R.id.btnR8, R.id.btnR16, R.id.btnR20, R.id.btnR25};
        int[] values = {4, 8, 16, 20, 25};
        for (int i = 0; i < ids.length; i++) {
            final int value = values[i];
            findViewById(ids[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setRounds(value);
                }
            });
        }
    }

    private void stepRounds(int delta) {
        setRounds(Num.rounds(inRounds.getText().toString()) + delta);
    }

    private void setRounds(int value) {
        int v = Num.clamp(value, 0, Num.MAX_ROUNDS);
        inRounds.setText(Num.field(v));
        inRounds.setSelection(inRounds.getText().length());
    }

    private void load() {
        Report r = db.byDate(date);
        if (r != null) {
            fillForm(r, true);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // Новый день: берём данные последнего отчёта, чтобы не заполнять с нуля.
            Report prev = db.previousBefore(date);
            if (prev != null) {
                fillForm(prev, false);
                if (prev.rounds <= 0) inRounds.setText(String.valueOf(Report.TARGET_ROUNDS));
                Toast.makeText(this, "Поля заполнены из отчёта за " + shortDate(prev.date),
                        Toast.LENGTH_LONG).show();
            } else {
                // Самый первый отчёт: подставляем цель по кругам, чтобы «пусто» не значило 0.
                inRounds.setText(String.valueOf(Report.TARGET_ROUNDS));
                inReading.setText("");
                cbWake.setChecked(false);
                cbSleep.setChecked(false);
                cbArati.setChecked(false);
                cbLecture.setChecked(false);
                cbPrasadam.setChecked(false);
                inService.setText("");
                inNotes.setText("");
            }
            btnDelete.setVisibility(View.GONE);
        }
        updateDateLabel();
    }

    /** Переносит значения отчёта в форму. Заметки — только когда это тот же день. */
    private void fillForm(Report r, boolean withNotes) {
        inRounds.setText(Num.field(r.rounds));
        inReading.setText(Num.field(r.readingMin));
        cbWake.setChecked(r.wakeEarly);
        cbSleep.setChecked(r.sleepEarly);
        cbArati.setChecked(r.mangalaArati);
        cbLecture.setChecked(r.lecture);
        cbPrasadam.setChecked(r.prasadam);
        inService.setText(r.service);
        inNotes.setText(withNotes ? r.notes : "");
    }

    private String shortDate(String isoDate) {
        return new SimpleDateFormat("d MMMM", new Locale("ru", "RU")).format(parseIso(isoDate));
    }

    private void updateDateLabel() {
        dateView.setText(human.format(parseIso(date)));
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        c.setTime(parseIso(date));
        DatePickerDialog dlg = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, dayOfMonth);
                date = iso.format(picked.getTime());
                updateDateLabel();
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void save() {
        final Report r = collect();
        if (!date.equals(originalDate) && db.byDate(date) != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Запись уже есть")
                    .setMessage("За " + human.format(parseIso(date)) + " отчёт уже существует. Заменить его?")
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Заменить", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            persist(r);
                        }
                    })
                    .show();
            return;
        }
        persist(r);
    }

    private void persist(Report r) {
        r.updatedAt = System.currentTimeMillis();
        db.upsert(r);

        // Проверяем не форму, а то, что реально легло в базу.
        Report stored = db.byDate(r.date);
        if (stored == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Не сохранилось")
                    .setMessage("Запись не найдена в базе после сохранения. Попробуйте ещё раз.")
                    .setPositiveButton("ОК", null)
                    .show();
            return;
        }
        Toast.makeText(this,
                "Сохранено: " + stored.rounds + " кругов, " + stored.readingMin + " мин · "
                        + human.format(parseIso(stored.date)),
                Toast.LENGTH_LONG).show();
        finish();
    }

    private Report collect() {
        Report r = new Report();
        r.date = date;
        r.rounds = Num.rounds(inRounds.getText().toString());
        r.readingMin = Num.minutes(inReading.getText().toString());
        r.wakeEarly = cbWake.isChecked();
        r.sleepEarly = cbSleep.isChecked();
        r.mangalaArati = cbArati.isChecked();
        r.lecture = cbLecture.isChecked();
        r.prasadam = cbPrasadam.isChecked();
        r.service = inService.getText().toString().trim();
        r.notes = inNotes.getText().toString().trim();
        return r;
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить отчёт?")
                .setMessage(human.format(parseIso(date)))
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.delete(date);
                        finish();
                    }
                })
                .show();
    }

    private Date parseIso(String s) {
        try {
            Date d = iso.parse(s);
            return d != null ? d : new Date();
        } catch (Exception e) {
            return new Date();
        }
    }
}
