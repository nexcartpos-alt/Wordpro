package wordpro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/** Modal picture editor: drag to select and crop, rotate, flip, brightness, contrast, grayscale, sepia, invert. */
class ImageEditor extends JDialog {
    private BufferedImage base, small, shown, result;
    private final BufferedImage original;
    private int mode;                       // 0 none, 1 grayscale, 2 sepia, 3 invert
    private Rectangle2D.Double crop;        // selection as fractions (0..1) of the preview
    private final JSlider bright = new JSlider(-100, 100, 0), contrast = new JSlider(-100, 100, 0);
    private final JLabel info = new JLabel(" ");
    private final Preview view = new Preview();

    ImageEditor(Frame owner, BufferedImage img) {
        super(owner, "Edit picture", true);
        original = img;
        base = img;
        bright.addChangeListener(e -> refresh());
        contrast.addChangeListener(e -> refresh());

        JPanel side = new JPanel(new GridLayout(0, 1, 4, 4));
        side.setBorder(new EmptyBorder(10, 10, 10, 10));
        side.add(btn("Rotate left", () -> { base = ImageOps.rotate(base, -1); rebuild(); }));
        side.add(btn("Rotate right", () -> { base = ImageOps.rotate(base, 1); rebuild(); }));
        side.add(btn("Flip horizontal", () -> { base = ImageOps.flip(base, true); rebuild(); }));
        side.add(btn("Flip vertical", () -> { base = ImageOps.flip(base, false); rebuild(); }));
        side.add(btn("Crop to selection", this::applyCrop));
        side.add(btn("Original colours", () -> { mode = 0; refresh(); }));
        side.add(btn("Grayscale", () -> { mode = 1; refresh(); }));
        side.add(btn("Sepia", () -> { mode = 2; refresh(); }));
        side.add(btn("Invert", () -> { mode = 3; refresh(); }));
        side.add(new JLabel("Brightness"));
        side.add(bright);
        side.add(new JLabel("Contrast"));
        side.add(contrast);
        side.add(btn("Reset all", this::resetAll));
        side.add(btn("OK", () -> { result = ImageOps.adjust(base, bright.getValue(), contrast.getValue(), mode); dispose(); }));
        side.add(btn("Cancel", this::dispose));

        info.setBorder(new EmptyBorder(6, 10, 8, 10));
        add(view, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
        add(info, BorderLayout.SOUTH);
        rebuild();
        pack();
        setLocationRelativeTo(owner);
    }

    /** Shows the editor and returns the edited picture, or null if cancelled. */
    BufferedImage showDialog() {
        setVisible(true);
        return result;
    }

    private JButton btn(String text, Runnable r) {
        JButton b = new JButton(text);
        b.addActionListener(e -> r.run());
        return b;
    }

    private void resetAll() {
        base = original;
        mode = 0;
        bright.setValue(0);
        contrast.setValue(0);
        rebuild();
    }

    private void rebuild() {
        small = ImageOps.fit(base, 640, 440);
        crop = null;
        refresh();
    }

    private void refresh() {
        shown = ImageOps.adjust(small, bright.getValue(), contrast.getValue(), mode);
        info.setText(base.getWidth() + " x " + base.getHeight() + " px   |   drag on the picture to choose a crop area");
        view.repaint();
    }

    private void applyCrop() {
        if (crop == null) return;
        int x = (int) Math.round(crop.x * base.getWidth()), y = (int) Math.round(crop.y * base.getHeight());
        int w = (int) Math.round(crop.width * base.getWidth()), h = (int) Math.round(crop.height * base.getHeight());
        crop = null;
        if (w < 2 || h < 2) { view.repaint(); return; }
        base = ImageOps.crop(base, x, y, w, h);
        rebuild();
    }

    private final class Preview extends JPanel {
        private Rectangle box = new Rectangle();
        private Point anchor;

        Preview() {
            setPreferredSize(new Dimension(640, 440));
            setBackground(new Color(0x3a3a3a));
            MouseAdapter m = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    anchor = box.contains(e.getPoint()) ? e.getPoint() : null;
                    crop = null;
                    repaint();
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (anchor == null || box.width == 0 || box.height == 0) return;
                    int x0 = Math.max(box.x, Math.min(anchor.x, e.getX())), x1 = Math.min(box.x + box.width, Math.max(anchor.x, e.getX()));
                    int y0 = Math.max(box.y, Math.min(anchor.y, e.getY())), y1 = Math.min(box.y + box.height, Math.max(anchor.y, e.getY()));
                    crop = new Rectangle2D.Double((x0 - box.x) / (double) box.width, (y0 - box.y) / (double) box.height,
                            (x1 - x0) / (double) box.width, (y1 - y0) / (double) box.height);
                    repaint();
                }
                @Override public void mouseReleased(MouseEvent e) {
                    if (crop != null && (crop.width * box.width < 4 || crop.height * box.height < 4)) crop = null;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (shown == null) return;
            double s = Math.min((double) getWidth() / shown.getWidth(), (double) getHeight() / shown.getHeight());
            int w = (int) (shown.getWidth() * s), h = (int) (shown.getHeight() * s);
            box = new Rectangle((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(shown, box.x, box.y, w, h, null);
            if (crop != null) {
                Rectangle r = new Rectangle(box.x + (int) (crop.x * w), box.y + (int) (crop.y * h), (int) (crop.width * w), (int) (crop.height * h));
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(box.x, box.y, w, r.y - box.y);
                g2.fillRect(box.x, r.y + r.height, w, box.y + h - r.y - r.height);
                g2.fillRect(box.x, r.y, r.x - box.x, r.height);
                g2.fillRect(r.x + r.width, r.y, box.x + w - r.x - r.width, r.height);
                g2.setColor(Color.WHITE);
                g2.drawRect(r.x, r.y, r.width, r.height);
            }
            g2.dispose();
        }
    }
}
