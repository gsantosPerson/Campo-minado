package br.com.gustavo.campominado;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Ranking {
    public static class Entry {
        public final long millis, date;
        Entry(long millis, long date) { this.millis = millis; this.date = date; }
    }
    private final ArrayList<Entry> entries = new ArrayList<>();

    public void add(long millis, long date) {
        if (millis < 0 || date <= 0) return;
        entries.add(new Entry(millis, date));
        Collections.sort(entries, (a, b) -> {
            int time = Long.compare(a.millis, b.millis);
            return time != 0 ? time : Long.compare(a.date, b.date);
        });
        while (entries.size() > 10) entries.remove(entries.size() - 1);
    }
    public List<Entry> entries() { return Collections.unmodifiableList(entries); }
    public String encode() {
        StringBuilder text = new StringBuilder();
        for (Entry entry : entries) text.append(entry.millis).append(',').append(entry.date).append(';');
        return text.toString();
    }
    public static Ranking decode(String text) {
        Ranking ranking = new Ranking();
        for (String row : text.split(";")) {
            String[] parts = row.split(",");
            if (parts.length != 2) continue;
            try { ranking.add(Long.parseLong(parts[0]), Long.parseLong(parts[1])); }
            catch (NumberFormatException ignored) { }
        }
        return ranking;
    }
}
