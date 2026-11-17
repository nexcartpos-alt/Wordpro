package wordpro;

import javax.swing.*;

/** Entry point. Build: ./build.sh  Run: java -cp out wordpro.Main */
public class Main {
    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "word-pro");
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
            new MainFrame().setVisible(true);
        });
    }
}
