package qms;

import javax.swing.*;

/** Window that shows the public "Now Serving" board and keeps it refreshed. */
public class DisplayBoard extends JFrame {
    private static final long serialVersionUID = 1L;
    private final transient Timer timer;

    public DisplayBoard(QueueService svc) {
        super("Live Display Board - Now Serving");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 640);
        setLocationByPlatform(true);
        BoardPanel panel = new BoardPanel(svc);
        add(panel);
        timer = new Timer(1500, e -> panel.refresh());
        timer.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { timer.stop(); }
        });
    }
}
