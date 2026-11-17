package wordpro;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/** Persistent user settings: recent files, canvas theme, AutoSave interval. */
public final class Prefs {
    private static final Preferences P = Preferences.userNodeForPackage(Prefs.class);
    private Prefs() { }

    public static List<String> recent() {
        List<String> l = new ArrayList<>();
        for (String s : P.get("recent", "").split("\\|")) if (!s.isEmpty() && new File(s).exists()) l.add(s);
        return l;
    }
    public static void addRecent(File f) {
        List<String> l = recent();
        l.remove(f.getPath());
        l.add(0, f.getPath());
        while (l.size() > 8) l.remove(l.size() - 1);
        P.put("recent", String.join("|", l));
    }
    public static List<String> recentSymbols() {
        List<String> l = new ArrayList<>();
        for (String s : P.get("symbols", "").split("\\|")) if (!s.isEmpty()) l.add(s);
        return l;
    }
    public static void addRecentSymbol(String s) {
        List<String> l = recentSymbols();
        l.remove(s);
        l.add(0, s);
        while (l.size() > 24) l.remove(l.size() - 1);
        P.put("symbols", String.join("|", l));
    }
    public static boolean darkCanvas() { return P.getBoolean("dark", false); }
    public static void setDarkCanvas(boolean b) { P.putBoolean("dark", b); }
    public static int autosaveMinutes() { return P.getInt("autosave", 2); }
    public static void setAutosaveMinutes(int m) { P.putInt("autosave", Math.max(0, m)); }
}
