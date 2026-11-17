package wordpro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/** The desk: a scrollable canvas holding a real paper sheet that grows as you type. */
class PageView extends JScrollPane {
    private final Editor ed;
    private final Canvas canvas = new Canvas();
    private final Page page = new Page();
    private int pw = 816, ph = 1056, mar = 96;   // paper size in px at 96 dpi (Letter by default)
    private boolean land;

    PageView(Editor ed) {
        this.ed = ed;
        ed.setBorder(new EmptyBorder(mar, mar, mar, mar));
        page.add(ed, BorderLayout.CENTER);
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTH; gc.weightx = 1; gc.weighty = 1;
        gc.insets = new Insets(24, 24, 40, 24);
        canvas.add(page, gc);
        canvas.setBackground(Prefs.darkCanvas() ? new Color(0x1e1e1e) : new Color(0xe6e6e6));
        ed.setGapColor(canvas.getBackground());
        setViewportView(canvas);
        setBorder(null);
        getVerticalScrollBar().setUnitIncrement(24);
        ed.setPageHeight(h());
        ed.onChange(page::revalidate);
        ed.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { page.revalidate(); }
        });
    }

    private int w() { return land ? ph : pw; }
    private int h() { return land ? pw : ph; }
    int usableWidth() { return w() - 2 * mar; }

    void setMargin(int m) { mar = m; ed.setBorder(new EmptyBorder(m, m, m, m)); relayout(); }
    void setPaper(int w, int h) { pw = w; ph = h; relayout(); }
    void setLandscape(boolean l) { land = l; relayout(); }
    void setCanvasColor(Color c) { canvas.setBackground(c); ed.setGapColor(c); }
    private void relayout() { ed.setPageHeight(h()); page.revalidate(); page.repaint(); }

    /** White sheet: at least one full page tall, then grows with the text. */
    private final class Page extends JPanel {
        Page() {
            super(new BorderLayout());
            setOpaque(false);
        }
        @Override public Dimension getPreferredSize() { return new Dimension(w(), Math.max(h(), ed.stackHeight())); }
    }

    private static final class Canvas extends JPanel implements Scrollable {
        Canvas() { super(new GridBagLayout()); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return (o == SwingConstants.VERTICAL ? r.height : r.width) - 40; }
        public boolean getScrollableTracksViewportWidth() { return getParent() instanceof JViewport && getParent().getWidth() > getPreferredSize().width; }
        public boolean getScrollableTracksViewportHeight() { return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height; }
    }
}
