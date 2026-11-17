package wordpro;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

/**
 * Vector icon set drawn with Java2D in one consistent line style (no image files).
 * Icons share a few bases (the sheet of paper, the text lines) with different overlays,
 * and every icon can be tinted: Icons.of("paper", 32, Color.GREEN).
 */
final class Icons {
    static final Color INK = new Color(0x4a4a4a), BLUE = new Color(0x2b579a), GREEN = new Color(0x2e8b57),
            ORANGE = new Color(0xed7d31), PURPLE = new Color(0x7b5fb5), RED = new Color(0xc00000),
            YELLOW = new Color(0xffd800), PAPER = Color.WHITE;
    private Icons() { }

    static Icon of(String name, int size) { return of(name, size, BLUE); }

    static Icon of(String name, int size, Color accent) {
        return new Icon() {
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                g2.scale(size / 32.0, size / 32.0);
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(g2, name, accent);
                g2.dispose();
            }
        };
    }

    // ---------- shared building blocks ----------
    private static void ln(Graphics2D g, double x1, double y1, double x2, double y2) { g.draw(new Line2D.Double(x1, y1, x2, y2)); }
    private static void sheet(Graphics2D g, double x, double y, double w, double h) {
        RoundRectangle2D r = new RoundRectangle2D.Double(x, y, w, h, 3, 3);
        g.setColor(PAPER); g.fill(r); g.setColor(INK); g.draw(r);
    }
    private static void rows(Graphics2D g, double x1, double x2, double y, int n, double gap) {
        g.setColor(INK);
        for (int i = 0; i < n; i++) ln(g, x1, y + i * gap, x2, y + i * gap);
    }
    private static void txt(Graphics2D g, String s, float size, double cx, double baseline, Color c) {
        g.setColor(c);
        g.setFont(new Font("SansSerif", Font.BOLD, 1).deriveFont(size));
        g.drawString(s, (float) (cx - g.getFontMetrics().stringWidth(s) / 2.0), (float) baseline);
    }
    private static void align(Graphics2D g, int t) { // 0 left, 1 center, 2 right, 3 justify
        double[] len = t == 3 ? new double[]{22, 22, 22, 22} : new double[]{22, 14, 22, 14};
        g.setColor(INK);
        for (int i = 0; i < 4; i++) {
            double x = (t == 0 || t == 3) ? 5 : t == 1 ? 16 - len[i] / 2 : 27 - len[i];
            ln(g, x, 7 + i * 6, x + len[i], 7 + i * 6);
        }
    }
    private static void dashed(Graphics2D g) {
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
    }
    private static void margin(Graphics2D g, Color a, double inset) {
        sheet(g, 6, 3, 20, 26);
        g.setColor(a); dashed(g);
        g.draw(new Rectangle2D.Double(6 + inset, 3 + inset, 20 - 2 * inset, 26 - 2 * inset));
    }
    private static void arrowHead(Graphics2D g, boolean right, double x, double y) {
        Path2D p = new Path2D.Double();
        double d = right ? 1 : -1;
        p.moveTo(x, y - 4); p.lineTo(x + 6 * d, y); p.lineTo(x, y + 4); p.closePath();
        g.fill(p);
    }

    // ---------- the icons ----------
    private static void draw(Graphics2D g, String n, Color a) {
        switch (n) {
            case "paste":
                g.setColor(a); g.fill(new RoundRectangle2D.Double(4, 6, 17, 22, 3, 3));
                g.setColor(PAPER); g.fill(new RoundRectangle2D.Double(9, 3, 8, 6, 2, 2));
                g.setColor(INK); g.draw(new RoundRectangle2D.Double(9, 3, 8, 6, 2, 2));
                sheet(g, 13, 11, 15, 18); rows(g, 16, 25, 17, 3, 4);
                break;
            case "copy":
                sheet(g, 5, 3, 15, 19); sheet(g, 12, 10, 15, 19); rows(g, 15, 24, 17, 3, 4);
                break;
            case "cut":
                g.setColor(INK);
                g.draw(new Ellipse2D.Double(6, 19, 7, 7)); g.draw(new Ellipse2D.Double(19, 19, 7, 7));
                ln(g, 9.5, 19, 21, 4); ln(g, 22.5, 19, 11, 4);
                break;
            case "undo":
                g.setColor(a);
                g.draw(new CubicCurve2D.Double(24, 24, 26, 13, 15, 10, 8, 14));
                ln(g, 8, 14, 12, 8); ln(g, 8, 14, 14, 18);
                break;
            case "redo":
                g.translate(32, 0); g.scale(-1, 1); draw(g, "undo", a);
                break;
            case "bullets":
                g.setColor(a);
                for (int i = 0; i < 3; i++) g.fill(new Ellipse2D.Double(4.5, 7 + i * 8, 4, 4));
                rows(g, 12, 27, 9, 3, 8);
                break;
            case "numbers":
                txt(g, "1", 9, 6.5, 12, a); txt(g, "2", 9, 6.5, 20, a); txt(g, "3", 9, 6.5, 28, a);
                rows(g, 12, 27, 9, 3, 8);
                break;
            case "indent":
                g.setColor(INK); ln(g, 4, 6, 28, 6); ln(g, 15, 12, 28, 12); ln(g, 15, 18, 28, 18); ln(g, 4, 24, 28, 24);
                g.setColor(a); arrowHead(g, true, 4, 15);
                break;
            case "outdent":
                g.setColor(INK); ln(g, 4, 6, 28, 6); ln(g, 15, 12, 28, 12); ln(g, 15, 18, 28, 18); ln(g, 4, 24, 28, 24);
                g.setColor(a); arrowHead(g, false, 11, 15);
                break;
            case "al-left": align(g, 0); break;
            case "al-center": align(g, 1); break;
            case "al-right": align(g, 2); break;
            case "al-justify": align(g, 3); break;
            case "find":
                g.setColor(INK); g.draw(new Ellipse2D.Double(5, 5, 15, 15));
                g.setColor(a); g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); ln(g, 18.5, 18.5, 27, 27);
                break;
            case "replace":
                g.setColor(a);
                ln(g, 5, 11, 25, 11); ln(g, 25, 11, 20, 6); ln(g, 25, 11, 20, 16);
                ln(g, 27, 22, 7, 22); ln(g, 7, 22, 12, 17); ln(g, 7, 22, 12, 27);
                break;
            case "picture": {
                sheet(g, 4, 6, 24, 20);
                g.setColor(YELLOW); g.fill(new Ellipse2D.Double(19, 9, 5, 5));
                Path2D p = new Path2D.Double();
                p.moveTo(6, 24); p.lineTo(13, 15); p.lineTo(18, 21); p.lineTo(21, 18); p.lineTo(26, 24);
                g.setColor(a); g.draw(p);
                break;
            }
            case "table":
                g.setColor(PAPER); g.fill(new Rectangle2D.Double(4, 7, 24, 19));
                g.setColor(a); g.fill(new Rectangle2D.Double(4, 7, 24, 6));
                g.setColor(INK); g.draw(new Rectangle2D.Double(4, 7, 24, 19));
                ln(g, 4, 13, 28, 13); ln(g, 4, 19.5, 28, 19.5); ln(g, 12, 7, 12, 26); ln(g, 20, 7, 20, 26);
                break;
            case "line":
                g.setColor(INK); ln(g, 4, 10, 4, 22); ln(g, 28, 10, 28, 22);
                g.setColor(a); g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); ln(g, 7, 16, 25, 16);
                break;
            case "date":
                g.setColor(PAPER); g.fill(new RoundRectangle2D.Double(4, 6, 24, 22, 3, 3));
                g.setColor(a); g.fill(new Rectangle2D.Double(5, 7, 22, 5));
                g.setColor(INK); g.draw(new RoundRectangle2D.Double(4, 6, 24, 22, 3, 3)); ln(g, 10, 3, 10, 9); ln(g, 22, 3, 22, 9);
                for (int r = 0; r < 2; r++) for (int c = 0; c < 3; c++) g.fill(new Ellipse2D.Double(8 + c * 6.5, 16 + r * 6, 3, 3));
                break;
            case "pagebreak": {
                Path2D top = new Path2D.Double(), bot = new Path2D.Double();
                top.moveTo(7, 13); top.lineTo(7, 4); top.lineTo(25, 4); top.lineTo(25, 13);
                bot.moveTo(7, 19); bot.lineTo(7, 28); bot.lineTo(25, 28); bot.lineTo(25, 19);
                g.setColor(INK); g.draw(top); g.draw(bot);
                g.setColor(a); dashed(g); ln(g, 3, 16, 29, 16);
                break;
            }
            case "margin-narrow": margin(g, a, 2); break;
            case "margin-normal": margin(g, a, 4); break;
            case "margin-wide": margin(g, a, 6.5); break;
            case "portrait": sheet(g, 9, 5, 14, 22); g.setColor(a); g.draw(new Rectangle2D.Double(12, 9, 8, 14)); break;
            case "landscape": sheet(g, 5, 9, 22, 14); g.setColor(a); g.draw(new Rectangle2D.Double(9, 12, 14, 8)); break;
            case "paper":
                sheet(g, 7, 3, 18, 26); g.setColor(a); g.fill(new Rectangle2D.Double(7.9, 3.9, 16.2, 5)); txt(g, "A", 13, 16, 24, a);
                break;
            case "wordcount":
                sheet(g, 7, 3, 18, 26); rows(g, 11, 21, 9, 2, 5); txt(g, "123", 8, 16, 25, a);
                break;
            case "dark":
                g.setColor(INK); g.draw(new Ellipse2D.Double(6, 6, 20, 20));
                g.setColor(a); g.fill(new Arc2D.Double(6, 6, 20, 20, 90, 180, Arc2D.PIE));
                break;
            case "maximize":
                g.setColor(INK);
                ln(g, 5, 12, 5, 5); ln(g, 5, 5, 12, 5); ln(g, 20, 5, 27, 5); ln(g, 27, 5, 27, 12);
                ln(g, 5, 20, 5, 27); ln(g, 5, 27, 12, 27); ln(g, 27, 20, 27, 27); ln(g, 20, 27, 27, 27);
                break;
            case "clear":
                txt(g, "A", 22, 13, 26, INK); g.setColor(RED); ln(g, 19, 6, 29, 16); ln(g, 29, 6, 19, 16);
                break;
            case "grow":
                txt(g, "A", 22, 13, 27, INK); g.setColor(a); ln(g, 26, 14, 26, 4); ln(g, 26, 4, 23, 8); ln(g, 26, 4, 29, 8);
                break;
            case "shrink":
                txt(g, "A", 22, 13, 27, INK); g.setColor(a); ln(g, 26, 4, 26, 14); ln(g, 26, 14, 23, 10); ln(g, 26, 14, 29, 10);
                break;
            case "highlight":
                g.setColor(YELLOW); g.fill(new RoundRectangle2D.Double(4, 23, 24, 6, 2, 2));
                g.setColor(INK); ln(g, 9, 19, 21, 7); ln(g, 21, 7, 25, 11); ln(g, 25, 11, 13, 23); ln(g, 13, 23, 9, 19);
                break;
            case "fontcolor":
                txt(g, "A", 24, 16, 22, INK); g.setColor(RED); g.fill(new Rectangle2D.Double(6, 25, 20, 4));
                break;
            case "case": txt(g, "Aa", 18, 16, 23, INK); break;
            case "sub": txt(g, "x", 20, 12, 22, INK); txt(g, "2", 12, 25, 29, a); break;
            case "sup": txt(g, "x", 20, 12, 28, INK); txt(g, "2", 12, 25, 13, a); break;
            case "imageedit":
                draw(g, "picture", a);
                g.setColor(ORANGE); ln(g, 18, 27, 27, 18); ln(g, 27, 18, 29, 20); ln(g, 29, 20, 20, 29); ln(g, 20, 29, 18, 27);
                break;
            case "symbol": txt(g, "\u03A9", 26, 16, 25, a); break;
            case "equation": txt(g, "\u03C0", 22, 10, 23, a); txt(g, "=", 17, 24, 22, INK); break;
            default: // "sheet" and anything unknown: the plain base icon
                sheet(g, 7, 3, 18, 26); rows(g, 11, 21, 10, 4, 5);
        }
    }
}
