package wordpro;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Non-modal Find and Replace window (Ctrl+F / Ctrl+H). */
public class FindDialog extends JDialog {
    private final JTextField find = new JTextField(22), repl = new JTextField(22);

    public FindDialog(Frame owner, Editor ed) {
        super(owner, "Find and Replace", false);
        JCheckBox mc = new JCheckBox("Match case");
        JLabel msg = new JLabel(" ");
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(new EmptyBorder(12, 12, 4, 12));
        form.add(new JLabel("Find what:")); form.add(find);
        form.add(new JLabel("Replace with:")); form.add(repl);
        form.add(mc); form.add(msg);

        JButton next = new JButton("Find Next"), prev = new JButton("Find Previous"),
                rep = new JButton("Replace"), all = new JButton("Replace All");
        next.addActionListener(e -> msg.setText(ed.find(find.getText(), mc.isSelected(), true) ? " " : "Not found"));
        prev.addActionListener(e -> msg.setText(ed.find(find.getText(), mc.isSelected(), false) ? " " : "Not found"));
        rep.addActionListener(e -> {
            String sel = ed.getSelectedText(), f = find.getText();
            if (sel != null && (mc.isSelected() ? sel.equals(f) : sel.equalsIgnoreCase(f))) ed.replaceSelected(repl.getText());
            msg.setText(ed.find(f, mc.isSelected(), true) ? " " : "Not found");
        });
        all.addActionListener(e -> msg.setText(ed.replaceAll(find.getText(), repl.getText(), mc.isSelected()) + " replaced"));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        for (JButton b : new JButton[]{next, prev, rep, all}) btns.add(b);
        getRootPane().setDefaultButton(next);
        add(form, BorderLayout.CENTER);
        add(btns, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    public void open(String preset) {
        if (preset != null && !preset.isEmpty()) find.setText(preset);
        setVisible(true);
        find.requestFocusInWindow();
        find.selectAll();
    }
}
