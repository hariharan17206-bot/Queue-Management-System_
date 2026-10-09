package qms;

import java.awt.*;
import java.nio.file.Paths;
import javax.swing.*;

/**
 * Queue Management System for College Services.
 * Run this class (Run As > Java Application) to start the application.
 */
public class App implements Nav {
    private final QueueService svc = new QueueService(new DataStore(Paths.get("data")));
    private final JFrame frame = new JFrame("Queue Management System - College Services");
    private DisplayBoard board;

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new App().start());
    }

    private void start() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1120, 700);
        frame.setLocationRelativeTo(null);
        logout();
        frame.setVisible(true);
    }

    private void show(JPanel p) {
        frame.setContentPane(p);
        frame.revalidate();
        frame.repaint();
    }

    @Override public void showStudent(User u) { show(new StudentPanel(svc, u, this)); }

    @Override public void showStaff(User u) { show(new StaffPanel(svc, u, this)); }

    @Override public void logout() { show(new LoginPanel(svc, this)); }

    @Override
    public void openBoard() {
        if (board == null || !board.isDisplayable()) board = new DisplayBoard(svc);
        board.setVisible(true);
        board.toFront();
    }
}
