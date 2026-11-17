package wordpro;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/** A picture in the document: keeps the original pixels, the current (edited) pixels and the display width. */
final class PicIcon implements Icon {
    final BufferedImage orig, img;
    final int w, h;
    private BufferedImage disp;

    PicIcon(BufferedImage img, int w) { this(img, img, w); }

    PicIcon(BufferedImage orig, BufferedImage img, int w) {
        this.orig = orig;
        this.img = img;
        this.w = Math.max(8, w);
        this.h = Math.max(8, (int) Math.round(this.w * (double) img.getHeight() / img.getWidth()));
    }

    PicIcon withWidth(int nw) { return new PicIcon(orig, img, nw); }
    PicIcon withImage(BufferedImage n, int nw) { return new PicIcon(orig, n, nw); }

    public int getIconWidth() { return w; }
    public int getIconHeight() { return h; }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (disp == null) disp = ImageOps.scale(img, w, h);
        g.drawImage(disp, x, y, null);
    }

    /** Embedded picture for HTML export. */
    static String html(PicIcon p) {
        try {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            ImageIO.write(p.img, "png", o);
            return "<img src=\"data:image/png;base64," + Base64.getEncoder().encodeToString(o.toByteArray())
                    + "\" width=\"" + p.w + "\" height=\"" + p.h + "\">";
        } catch (IOException e) { return ""; }
    }
}
