package wordpro;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.*;

/**
 * Unicode math and text symbols grouped by category. Each symbol's name doubles as a LaTeX-style
 * shortcut: type \alpha then space and the editor swaps it for the symbol.
 * This source is ASCII-only (\\u escapes) so it compiles correctly on every platform encoding.
 */
final class Symbols {
    static final class Sym {
        final String ch, name, cat;
        Sym(String ch, String name, String cat) { this.ch = ch; this.name = name; this.cat = cat; }
        String hex() {
            StringBuilder b = new StringBuilder();
            ch.codePoints().forEach(c -> b.append(b.length() == 0 ? "" : " ").append(String.format("U+%04X", c)));
            return b.toString();
        }
    }

    private static final String[][] DATA = {
        {"Greek", "\u03b1 alpha|\u03b2 beta|\u03b3 gamma|\u03b4 delta|\u03b5 epsilon|\u03b6 zeta|\u03b7 eta|\u03b8 theta|\u03b9 iota|\u03ba kappa|\u03bb lambda|\u03bc mu|\u03bd nu|\u03be xi|\u03bf omicron|\u03c0 pi|\u03c1 rho|\u03c3 sigma|\u03c2 varsigma|\u03c4 tau|\u03c5 upsilon|\u03c6 phi|\u03c7 chi|\u03c8 psi|\u03c9 omega|\u03d1 vartheta|\u03d5 varphi|\u03d6 varpi|\u03f1 varrho|\u03f5 varepsilon|\u0393 Gamma|\u0394 Delta|\u0398 Theta|\u039b Lambda|\u039e Xi|\u03a0 Pi|\u03a3 Sigma|\u03a5 Upsilon|\u03a6 Phi|\u03a8 Psi|\u03a9 Omega"},
        {"Operators", "\u00b1 pm|\u2213 mp|\u00d7 times|\u00f7 div|\u00b7 cdot|\u2217 ast|\u2218 circ|\u2022 bullet|\u2295 oplus|\u2296 ominus|\u2297 otimes|\u2299 odot|\u221a sqrt|\u221b cbrt|\u221c fourthroot|\u2211 sum|\u220f prod|\u2210 coprod|\u222b int|\u222c iint|\u222d iiint|\u222e oint|\u2202 partial|\u2207 nabla|\u221e infty|\u2032 prime|\u2033 dprime|\u00b0 degree|\u221d propto|\u2214 dotplus"},
        {"Relations", "\u2264 leq|\u2265 geq|\u2260 neq|\u2248 approx|\u2261 equiv|\u2262 nequiv|\u2245 cong|\u223c sim|\u2243 simeq|\u226a ll|\u226b gg|\u227a prec|\u227b succ|\u2225 parallel|\u22a5 perp|\u2223 mid|\u2224 nmid|\u2254 coloneqq|\u225c triangleq|\u2250 doteq|\u2272 lesssim|\u2273 gtrsim|\u226e nless|\u226f ngtr|\u2270 nleq|\u2271 ngeq|\u2249 napprox"},
        {"Sets & Logic", "\u2208 in|\u2209 notin|\u220b ni|\u2282 subset|\u2283 supset|\u2286 subseteq|\u2287 supseteq|\u2284 nsubset|\u228a subsetneq|\u222a cup|\u2229 cap|\u2205 emptyset|\u2216 setminus|\u2115 bbN|\u2124 bbZ|\u211a bbQ|\u211d bbR|\u2102 bbC|\u2119 bbP|\u2200 forall|\u2203 exists|\u2204 nexists|\u00ac neg|\u2227 wedge|\u2228 vee|\u22bb veebar|\u22a4 top|\u22a2 vdash|\u22a8 models|\u2234 therefore|\u2235 because|\u220e qed|\u21d2 implies|\u21d4 iff"},
        {"Arrows", "\u2190 leftarrow|\u2192 rightarrow|\u2192 to|\u2191 uparrow|\u2193 downarrow|\u2194 leftrightarrow|\u2195 updownarrow|\u21d0 Leftarrow|\u21d2 Rightarrow|\u21d1 Uparrow|\u21d3 Downarrow|\u21d4 Leftrightarrow|\u21a6 mapsto|\u27f6 longrightarrow|\u27f5 longleftarrow|\u27f7 longleftrightarrow|\u27f9 Longrightarrow|\u21cc rightleftharpoons|\u2197 nearrow|\u2198 searrow|\u2196 nwarrow|\u2199 swarrow|\u21a9 hookleftarrow|\u21aa hookrightarrow|\u21bb circlearrowright|\u21ba circlearrowleft"},
        {"Superscripts", "\u2070 sup0|\u00b9 sup1|\u00b2 sup2|\u00b3 sup3|\u2074 sup4|\u2075 sup5|\u2076 sup6|\u2077 sup7|\u2078 sup8|\u2079 sup9|\u207a supplus|\u207b supminus|\u207c supequal|\u207d supopen|\u207e supclose|\u207f supn|\u2071 supi"},
        {"Subscripts", "\u2080 sub0|\u2081 sub1|\u2082 sub2|\u2083 sub3|\u2084 sub4|\u2085 sub5|\u2086 sub6|\u2087 sub7|\u2088 sub8|\u2089 sub9|\u208a subplus|\u208b subminus|\u208c subequal|\u208d subopen|\u208e subclose|\u2090 suba|\u2091 sube|\u2092 subo|\u2093 subx|\u2099 subn|\u1d62 subi|\u2c7c subj|\u2096 subk"},
        {"Fractions & Misc", "\u00bd half|\u00bc quarter|\u00be threequarters|\u2153 third|\u2154 twothirds|\u2155 fifth|\u2159 sixth|\u215b eighth|\u215c threeeighths|\u215d fiveeighths|\u215e seveneighths|\u2135 aleph|\u2136 beth|\u210f hbar|\u2113 ell|\u2118 wp|\u211c Re|\u2111 Im|\u2220 angle|\u2221 measuredangle|\u221f rightangle|\u25b3 triangle|\u25a1 square|\u25a0 blacksquare|\u25c7 diamond|\u230a lfloor|\u230b rfloor|\u2308 lceil|\u2309 rceil|\u27e8 langle|\u27e9 rangle|\u2016 norm|\u22ef cdots|\u2026 ldots|\u22ee vdots|\u22f1 ddots"},
        {"Text & Currency", "\u20ac euro|\u00a3 pound|\u00a5 yen|\u00a2 cent|\u20a8 rupee|\u20b9 indianrupee|\u20bf bitcoin|\u00a9 copyright|\u00ae registered|\u2122 trademark|\u00a7 section|\u00b6 paragraph|\u2020 dagger|\u2021 ddagger|\u2013 endash|\u2014 emdash|\u2018 lsquo|\u2019 rsquo|\u201c ldquo|\u201d rdquo|\u00ab laquo|\u00bb raquo|\u2022 bullet2|\u2713 checkmark|\u2717 xmark|\u2605 star|\u2606 staroutline|\u2665 heart|\u2666 diamondsuit|\u2660 spadesuit|\u2663 clubsuit|\u2116 numero|\u2030 permille|\u00b5 micro|\u00c5 angstrom|\u03a9 ohm|\u2103 celsius|\u2109 fahrenheit"}
    };
    static final String QUICK = "\u00b1 \u00d7 \u00f7 \u221a \u2211 \u222b \u221e \u03c0 \u03b8 \u03b1 \u03b2 \u0394 \u2264 \u2265 \u2260 \u2248 \u2192 \u00b0 \u00b2 \u00b3 \u00bd \u2202";
    static final String[][] TEMPLATES = {
        {"Quadratic formula", "x = (\u2212b \u00b1 \u221a(b\u00b2 \u2212 4ac)) / 2a"},
        {"Pythagorean theorem", "a\u00b2 + b\u00b2 = c\u00b2"},
        {"Sum of integers", "\u2211\u1d62\u208c\u2081\u207f i = n(n + 1)/2"},
        {"Definite integral", "\u222b\u2090\u1d47 f(x) dx"},
        {"Derivative definition", "f\u2032(x) = lim(h\u21920) [f(x + h) \u2212 f(x)] / h"},
        {"Partial derivative", "\u2202f/\u2202x"},
        {"Chain rule", "dy/dx = (dy/du)(du/dx)"},
        {"Euler's identity", "e^(i\u03c0) + 1 = 0"},
        {"Limit", "lim(x\u2192\u221e) 1/x = 0"},
        {"Normal distribution", "f(x) = 1/(\u03c3\u221a(2\u03c0)) \u00b7 e^(\u2212(x\u2212\u03bc)\u00b2/(2\u03c3\u00b2))"}
    };

    static final List<String> CATEGORIES = new ArrayList<>();
    private static final Map<String, List<Sym>> BY_CAT = new LinkedHashMap<>();
    private static final Map<String, String> BY_NAME = new HashMap<>();
    private static final String[] PREFERRED = {"Cambria Math", "Segoe UI Symbol", "STIX Two Math", "Noto Sans Math",
            "DejaVu Sans", "Apple Symbols", "Arial Unicode MS", "Lucida Sans Unicode"};
    private static Set<String> families;

    static {
        for (String[] row : DATA) {
            List<Sym> l = new ArrayList<>();
            for (String e : row[1].split("\\|")) {
                int i = e.indexOf(' ');
                Sym s = new Sym(e.substring(0, i), e.substring(i + 1), row[0]);
                l.add(s);
                if (!BY_NAME.containsKey(s.name)) BY_NAME.put(s.name, s.ch);
            }
            CATEGORIES.add(row[0]);
            BY_CAT.put(row[0], l);
        }
    }
    private Symbols() { }

    static List<Sym> in(String cat) {
        List<Sym> l = BY_CAT.get(cat);
        return l == null ? new ArrayList<Sym>() : l;
    }
    static List<Sym> all() {
        List<Sym> l = new ArrayList<>();
        for (List<Sym> c : BY_CAT.values()) l.addAll(c);
        return l;
    }
    static List<Sym> search(String q) {
        String k = q.trim().toLowerCase(Locale.ROOT);
        List<Sym> l = new ArrayList<>();
        for (Sym s : all()) if (s.name.toLowerCase(Locale.ROOT).contains(k) || s.ch.equals(q.trim()) || s.cat.toLowerCase(Locale.ROOT).contains(k)) l.add(s);
        return l;
    }
    static List<Sym> fromChars(List<String> chars) {
        List<Sym> known = all(), l = new ArrayList<>();
        for (String c : chars) {
            Sym hit = null;
            for (Sym s : known) if (s.ch.equals(c)) { hit = s; break; }
            l.add(hit != null ? hit : new Sym(c, "", "Recent"));
        }
        return l;
    }
    /** LaTeX-style lookup: "alpha" returns the alpha sign, or null. */
    static String byName(String name) { return BY_NAME.get(name); }

    private static synchronized Set<String> families() {
        if (families == null) families = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        return families;
    }
    /** Best font for showing symbols in the symbol window. */
    static String uiFamily() {
        for (String f : PREFERRED) if (families().contains(f)) return f;
        return "SansSerif";
    }
    /** Keeps the current font when it has every glyph in s; otherwise picks a font that does. */
    static String fontFor(String s, String current) {
        if (new Font(current, Font.PLAIN, 12).canDisplayUpTo(s) == -1) return current;
        for (String f : PREFERRED) if (families().contains(f) && new Font(f, Font.PLAIN, 12).canDisplayUpTo(s) == -1) return f;
        return current;
    }
}
