package wordpro;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Pixel operations used by pasting, the context menu and the picture editor. */
final class ImageOps {
    private ImageOps() { }

    /** Any AWT image to a fully loaded ARGB BufferedImage (null if it has no pixels). */
    static BufferedImage argb(Image i) {
        if (i instanceof BufferedImage && ((BufferedImage) i).getType() == BufferedImage.TYPE_INT_ARGB) return (BufferedImage) i;
        ImageIcon ic = new ImageIcon(i);
        int w = ic.getIconWidth(), h = ic.getIconHeight();
        if (w <= 0 || h <= 0) return null;
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = b.createGraphics();
        g.drawImage(ic.getImage(), 0, 0, null);
        g.dispose();
        return b;
    }

    private static BufferedImage draw(BufferedImage s, int w, int h) {
        BufferedImage d = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = d.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(s, 0, 0, d.getWidth(), d.getHeight(), null);
        g.dispose();
        return d;
    }

    /** Smooth resize (halves repeatedly for big reductions). */
    static BufferedImage scale(BufferedImage s, int w, int h) {
        BufferedImage cur = s;
        int cw = s.getWidth(), ch = s.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) { cw /= 2; ch /= 2; cur = draw(cur, cw, ch); }
        return draw(cur, w, h);
    }

    /** Shrinks to fit inside a box; never enlarges. */
    static BufferedImage fit(BufferedImage s, int maxW, int maxH) {
        double k = Math.min(1.0, Math.min((double) maxW / s.getWidth(), (double) maxH / s.getHeight()));
        return k >= 1.0 ? s : scale(s, (int) Math.round(s.getWidth() * k), (int) Math.round(s.getHeight() * k));
    }

    /** Rotates by quarter turns (positive = clockwise). */
    static BufferedImage rotate(BufferedImage s, int quarters) {
        int q = ((quarters % 4) + 4) % 4;
        if (q == 0) return s;
        int w = s.getWidth(), h = s.getHeight();
        boolean swap = q % 2 == 1;
        BufferedImage d = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = d.createGraphics();
        AffineTransform t = new AffineTransform();
        t.translate(d.getWidth() / 2.0, d.getHeight() / 2.0);
        t.rotate(Math.PI / 2 * q);
        t.translate(-w / 2.0, -h / 2.0);
        g.drawImage(s, t, null);
        g.dispose();
        return d;
    }

    static BufferedImage flip(BufferedImage s, boolean horizontal) {
        int w = s.getWidth(), h = s.getHeight();
        BufferedImage d = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = d.createGraphics();
        g.drawImage(s, horizontal ? new AffineTransform(-1, 0, 0, 1, w, 0) : new AffineTransform(1, 0, 0, -1, 0, h), null);
        g.dispose();
        return d;
    }

    static BufferedImage crop(BufferedImage s, int x, int y, int w, int h) {
        x = Math.max(0, Math.min(x, s.getWidth() - 1));
        y = Math.max(0, Math.min(y, s.getHeight() - 1));
        w = Math.max(1, Math.min(w, s.getWidth() - x));
        h = Math.max(1, Math.min(h, s.getHeight() - y));
        BufferedImage d = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = d.createGraphics();
        g.drawImage(s.getSubimage(x, y, w, h), 0, 0, null);
        g.dispose();
        return d;
    }

    private static int cl(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }

    /** brightness and contrast: -100..100. mode: 0 none, 1 grayscale, 2 sepia, 3 invert. */
    static BufferedImage adjust(BufferedImage s, int brightness, int contrast, int mode) {
        if (brightness == 0 && contrast == 0 && mode == 0) return s;
        int w = s.getWidth(), h = s.getHeight();
        int[] px = s.getRGB(0, 0, w, h, null, 0, w);
        double c = contrast * 2.55, f = 259 * (c + 255) / (255 * (259 - c)), b = brightness * 2.55;
        for (int i = 0; i < px.length; i++) {
            int p = px[i], a = p >>> 24;
            double r = (p >> 16) & 255, g = (p >> 8) & 255, bl = p & 255;
            if (mode == 1) {
                r = g = bl = 0.299 * r + 0.587 * g + 0.114 * bl;
            } else if (mode == 2) {
                double nr = 0.393 * r + 0.769 * g + 0.189 * bl, ng = 0.349 * r + 0.686 * g + 0.168 * bl, nb = 0.272 * r + 0.534 * g + 0.131 * bl;
                r = nr; g = ng; bl = nb;
            } else if (mode == 3) {
                r = 255 - r; g = 255 - g; bl = 255 - bl;
            }
            px[i] = (a << 24) | (cl(f * (r - 128) + 128 + b) << 16) | (cl(f * (g - 128) + 128 + b) << 8) | cl(f * (bl - 128) + 128 + b);
        }
        BufferedImage d = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        d.setRGB(0, 0, w, h, px, 0, w);
        return d;
    }

    /** Saves as png (default), jpg, bmp or gif by file extension. */
    static void save(BufferedImage img, File f) throws IOException {
        String n = f.getName().toLowerCase();
        if (!n.contains(".")) { f = new File(f.getPath() + ".png"); n = f.getName().toLowerCase(); }
        String fmt = n.endsWith(".jpg") || n.endsWith(".jpeg") ? "jpg" : n.endsWith(".bmp") ? "bmp" : n.endsWith(".gif") ? "gif" : "png";
        BufferedImage out = img;
        if (!fmt.equals("png")) {
            out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());
            g.drawImage(img, 0, 0, null);
            g.dispose();
        }
        if (!ImageIO.write(out, fmt, f)) throw new IOException("No writer for " + fmt);
    }
}
