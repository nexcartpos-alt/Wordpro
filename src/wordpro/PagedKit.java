package wordpro;

import javax.swing.SizeRequirements;
import javax.swing.text.*;
import javax.swing.text.rtf.RTFEditorKit;

/**
 * RTF editor kit with a page-aware layout: paragraphs never straddle the gap between pages
 * (they move to the top of the next page) and a "Page break" paragraph forces a new page.
 */
final class PagedKit extends RTFEditorKit {
    private final Editor ed;
    private final ViewFactory base;

    PagedKit(Editor ed) {
        this.ed = ed;
        this.base = super.getViewFactory();
    }

    @Override public ViewFactory getViewFactory() {
        return elem -> AbstractDocument.SectionElementName.equals(elem.getName()) ? new PagedView(elem, ed) : base.create(elem);
    }

    private static final class PagedView extends BoxView {
        private final Editor ed;

        PagedView(Element e, Editor ed) {
            super(e, View.Y_AXIS);
            this.ed = ed;
        }

        /** Where a block of height h may start, given the current y (view coordinates). */
        private int fit(int y, int h) {
            int c = ed.pageContent(), s = ed.pageStride();
            if (c <= 0) return y;
            int into = y % s;
            if (into >= c) return y - into + s;                        // landed in the gap
            if (into > 0 && h <= c && into + h > c) return y - into + s; // would straddle the gap
            return y;
        }

        private int nextPage(int y) {
            int s = ed.pageStride();
            return y % s == 0 ? y : (y / s + 1) * s;
        }

        private boolean isBreak(View v) {
            try {
                Element e = v.getElement();
                return e.getEndOffset() - e.getStartOffset() >= 3
                        && getDocument().getText(e.getStartOffset(), 3).equals(Editor.BREAK_PREFIX);
            } catch (BadLocationException ex) { return false; }
        }

        @Override protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            int y = 0;
            boolean force = false;
            for (int i = 0; i < offsets.length; i++) {
                View v = getView(i);
                int h = (int) v.getPreferredSpan(axis);
                y = force ? nextPage(y) : fit(y, h);
                offsets[i] = y;
                spans[i] = h;
                y += h;
                force = isBreak(v);
            }
        }

        @Override protected SizeRequirements calculateMajorAxisRequirements(int axis, SizeRequirements r) {
            if (r == null) r = new SizeRequirements();
            int y = 0;
            boolean force = false;
            for (int i = 0; i < getViewCount(); i++) {
                View v = getView(i);
                int h = (int) v.getPreferredSpan(axis);
                y = force ? nextPage(y) : fit(y, h);
                y += h;
                force = isBreak(v);
            }
            r.minimum = y;
            r.preferred = y;
            r.maximum = y;
            r.alignment = 0f;
            return r;
        }
    }
}
