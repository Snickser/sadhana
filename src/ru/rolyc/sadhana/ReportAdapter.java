package ru.rolyc.sadhana;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Список отчётов. */
public class ReportAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<Report> data = new ArrayList<Report>();
    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat human = new SimpleDateFormat("d MMMM yyyy, EEEE", new Locale("ru", "RU"));

    public ReportAdapter(Context ctx) {
        this.inflater = LayoutInflater.from(ctx);
    }

    public void setData(List<Report> reports) {
        data.clear();
        data.addAll(reports);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Report getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) v = inflater.inflate(R.layout.row_report, parent, false);
        Report r = data.get(position);

        TextView date = (TextView) v.findViewById(R.id.rowDate);
        TextView details = (TextView) v.findViewById(R.id.rowDetails);
        TextView marks = (TextView) v.findViewById(R.id.rowMarks);
        TextView rounds = (TextView) v.findViewById(R.id.rowRounds);

        // Сегодняшний день отмечен шафрановой полосой слева
        View bar = v.findViewById(R.id.rowBar);
        bar.setVisibility(isToday(r.date) ? View.VISIBLE : View.GONE);

        date.setText(humanDate(r.date));
        StringBuilder detailsText = new StringBuilder();
        detailsText.append("балл ").append(r.score()).append("/7");
        detailsText.append(r.readingMin > 0 ? " · чтение " + r.readingMin + " мин" : " · чтения нет");
        if (r.service != null && r.service.trim().length() > 0) detailsText.append(" · служение");
        details.setText(detailsText.toString());
        marks.setText(marksLine(r));
        rounds.setText(roundsLabel(r.rounds));

        return v;
    }

    /** «16» крупно и «круг.» мелким — одним текстом, без второго TextView. */
    private CharSequence roundsLabel(int value) {
        String unit = "\nкруг.";
        SpannableString s = new SpannableString(value + unit);
        s.setSpan(new RelativeSizeSpan(0.62f), String.valueOf(value).length(), s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private boolean isToday(String isoDate) {
        return iso.format(new Date()).equals(isoDate);
    }

    private String marksLine(Report r) {
        StringBuilder sb = new StringBuilder();
        sb.append("пункты ").append(r.checkedCount()).append("/5");
        if (r.wakeEarly) sb.append(" · 5:00");
        if (r.mangalaArati) sb.append(" · арати");
        if (r.lecture) sb.append(" · лекция");
        if (r.prasadam) sb.append(" · прасад");
        if (r.sleepEarly) sb.append(" · 22:00");
        if (r.service != null && r.service.trim().length() > 0) sb.append(" · служение");
        return sb.toString();
    }

    private String humanDate(String isoDate) {
        try {
            Date d = iso.parse(isoDate);
            if (d == null) return isoDate;
            Calendar today = Calendar.getInstance();
            String todayIso = iso.format(today.getTime());
            if (todayIso.equals(isoDate)) return "Сегодня, " + human.format(d);
            Calendar y = Calendar.getInstance();
            y.add(Calendar.DAY_OF_YEAR, -1);
            if (iso.format(y.getTime()).equals(isoDate)) return "Вчера, " + human.format(d);
            return human.format(d);
        } catch (ParseException e) {
            return isoDate;
        }
    }
}
