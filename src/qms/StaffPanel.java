package qms;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Staff / admin dashboard: call next, complete, skip, walk-in tokens and live statistics. */
public class StaffPanel extends JPanel {
    private final QueueService svc;
    private final Nav nav;
    private final JComboBox<ServiceType> serviceBox = new JComboBox<>(ServiceType.values());
    private final JComboBox<String> counterBox = new JComboBox<>();
    private final DefaultTableModel model = UI.model("Token", "Name", "Student ID", "Type", "Status", "Counter", "Issued (time)");
    private final JTable table = new JTable(model);
    private final JLabel total = new JLabel("0"), waiting = new JLabel("0"), serving = new JLabel("0"),
            completed = new JLabel("0"), skipped = new JLabel("0");
    private final JLabel countersInfo = new JLabel(" ");
    private final JLabel serviceStats = new JLabel(" ");
    private Timer timer;

    public StaffPanel(QueueService svc, User user, Nav nav) {
        this.svc = svc;
        this.nav = nav;
        setLayout(new BorderLayout(0, 10));
        setBackground(UI.BG);

        JButton logout = UI.button("Logout", UI.RED);
        JButton board = UI.button("Open Display Board", UI.AMBER);
        JButton demo = UI.button("Add Demo Data", new Color(0x4F5F8A));
        JButton clear = UI.button("Clear Today", new Color(0x4F5F8A));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(demo);
        right.add(clear);
        right.add(board);
        right.add(logout);
        demo.addActionListener(e -> { svc.seedDemo(); refresh(); });
        clear.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Delete all of today's tokens?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { svc.clearToday(); refresh(); }
        });

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(UI.header("Staff Dashboard", "Logged in as " + user.name + "  -  all services, today", right), BorderLayout.NORTH);
        top.add(buildStats(), BorderLayout.CENTER);
        top.add(buildControls(), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        UI.styleTable(table, 4);
        int[] w = {70, 170, 190, 90, 110, 100, 100};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new GridLayout(2, 1));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, 16, 10, 16));
        countersInfo.setFont(UI.BOLD);
        countersInfo.setForeground(UI.NAVY);
        serviceStats.setFont(UI.BASE);
        south.add(countersInfo);
        south.add(serviceStats);
        add(south, BorderLayout.SOUTH);

        board.addActionListener(e -> nav.openBoard());
        logout.addActionListener(e -> { timer.stop(); nav.logout(); });
        serviceBox.addActionListener(e -> { fillCounters(); refresh(); });
        fillCounters();
        timer = new Timer(2000, e -> refresh());
        timer.start();
        refresh();
    }

    private JPanel buildStats() {
        JPanel p = new JPanel(new GridLayout(1, 5, 12, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        p.add(stat("Total tokens", total, UI.NAVY));
        p.add(stat("Waiting", waiting, UI.AMBER));
        p.add(stat("Being served", serving, UI.TEAL));
        p.add(stat("Completed", completed, new Color(0x3D5A99)));
        p.add(stat("Skipped / Cancelled", skipped, UI.RED));
        return p;
    }

    private JPanel stat(String title, JLabel value, Color color) {
        JPanel c = UI.card(new BorderLayout());
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 12));
        t.setForeground(Color.DARK_GRAY);
        UI.statCard(title, value, color);
        c.add(t, BorderLayout.NORTH);
        c.add(value, BorderLayout.CENTER);
        return c;
    }

    private JPanel buildControls() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        JButton call = UI.button("Call Next", UI.TEAL);
        JButton done = UI.button("Complete", UI.NAVY);
        JButton skip = UI.button("Skip (No-show)", UI.RED);
        JButton walk = UI.button("Walk-in Token", UI.AMBER);
        JButton prio = UI.button("Priority On/Off", UI.RED);
        p.add(new JLabel("Service:"));
        p.add(serviceBox);
        p.add(new JLabel("Counter:"));
        p.add(counterBox);
        p.add(call);
        p.add(done);
        p.add(skip);
        p.add(walk);
        p.add(prio);

        call.addActionListener(e -> run(() -> svc.callNext(service(), counter())));
        done.addActionListener(e -> run(() -> svc.complete(current())));
        skip.addActionListener(e -> run(() -> svc.skip(current())));
        walk.addActionListener(e -> walkIn());
        prio.addActionListener(e -> togglePriority());
        return p;
    }

    private ServiceType service() { return (ServiceType) serviceBox.getSelectedItem(); }

    private String counter() { return (String) counterBox.getSelectedItem(); }

    private Token current() {
        Token t = svc.servingAt(service(), counter());
        if (t == null) throw new IllegalStateException(counter() + " is not serving anyone. Use 'Call Next' first.");
        return t;
    }

    private void fillCounters() {
        counterBox.removeAllItems();
        for (int i = 1; i <= service().counters; i++) counterBox.addItem("Counter " + i);
    }

    private void run(Runnable r) {
        try {
            r.run();
            refresh();
        } catch (IllegalStateException ex) {
            UI.error(this, ex.getMessage());
        }
    }

    /** Staff decide priority: select a waiting token in the table and toggle it. */
    private void togglePriority() {
        int r = table.getSelectedRow();
        if (r < 0) { UI.error(this, "Select a waiting token in the table first."); return; }
        String label = (String) model.getValueAt(r, 0);
        for (Token t : svc.queueView(service()))
            if (t.label().equals(label)) {
                run(() -> svc.setPriority(t, !t.priority));
                return;
            }
    }

    private void walkIn() {
        JTextField name = new JTextField();
        JCheckBox pr = new JCheckBox("Priority");
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 6));
        p.add(new JLabel("Visitor name (" + service().label + "):"));
        p.add(name);
        p.add(pr);
        if (JOptionPane.showConfirmDialog(this, p, "Walk-in Token", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            Token t = svc.issueWalkIn(name.getText(), service(), pr.isSelected());
            refresh();
            UI.info(this, "Walk-in token issued: " + t.label() + "\nPosition " + svc.position(t) + " in queue.");
        } catch (IllegalArgumentException ex) {
            UI.error(this, ex.getMessage());
        }
    }

    private void refresh() {
        QueueService.Stats all = svc.stats(null);
        total.setText(String.valueOf(all.total));
        waiting.setText(String.valueOf(all.waiting));
        serving.setText(String.valueOf(all.serving));
        completed.setText(String.valueOf(all.completed));
        skipped.setText((all.skipped) + " / " + all.cancelled);

        ServiceType s = service();
        int sel = table.getSelectedRow();
        String keep = sel < 0 ? null : (String) model.getValueAt(sel, 0);
        model.setRowCount(0);
        for (Token t : svc.queueView(s))
            model.addRow(new Object[]{t.label(), t.studentName, t.studentId, t.priority ? "PRIORITY" : "NORMAL",
                    t.status.name(), t.counter, t.issuedAt.substring(11)});
        if (keep != null)
            for (int r = 0; r < model.getRowCount(); r++)
                if (keep.equals(model.getValueAt(r, 0))) { table.setRowSelectionInterval(r, r); break; }

        StringBuilder sb = new StringBuilder(s.label + " counters:  ");
        for (int i = 1; i <= s.counters; i++) {
            Token t = svc.servingAt(s, "Counter " + i);
            sb.append("Counter ").append(i).append(": ").append(t == null ? "free" : t.label() + " (" + t.studentName + ")");
            if (i < s.counters) sb.append("   |   ");
        }
        countersInfo.setText(sb.toString());
        QueueService.Stats st = svc.stats(s);
        serviceStats.setText(String.format("%s today:  waiting %d,  completed %d,  avg service time %.1f min,  avg waiting time %.1f min",
                s.label, st.waiting, st.completed, st.avgServiceMin, st.avgWaitMin));
    }
}
