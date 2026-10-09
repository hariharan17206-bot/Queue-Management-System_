package qms;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Student dashboard: get a token, then track position and estimated waiting time live. */
public class StudentPanel extends JPanel {
    private final QueueService svc;
    private final User user;
    private final Nav nav;
    private final JComboBox<ServiceType> serviceBox = new JComboBox<>(ServiceType.values());
    private final JLabel liveInfo = new JLabel(" ");
    private final JLabel banner = new JLabel(" ");
    private final DefaultTableModel model = UI.model("Token", "Service", "Type", "Status", "Position", "Est. wait", "Counter", "Issued at");
    private final JTable table = new JTable(model);
    private final JTextArea detail = UI.detailArea();
    private final JTabbedPane tabs = new JTabbedPane();
    private final Set<String> notified = new HashSet<>();
    private Timer timer;

    public StudentPanel(QueueService svc, User user, Nav nav) {
        this.svc = svc;
        this.user = user;
        this.nav = nav;
        setLayout(new BorderLayout());
        setBackground(UI.BG);

        JButton logout = UI.button("Logout", UI.RED);
        JButton board = UI.button("Live Board", UI.AMBER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(board);
        right.add(logout);
        add(UI.header("Student Dashboard", "Welcome, " + user.name + "  (" + user.id + ")", right), BorderLayout.NORTH);
        board.addActionListener(e -> nav.openBoard());
        logout.addActionListener(e -> { timer.stop(); nav.logout(); });

        tabs.setFont(UI.BOLD);
        tabs.addTab("Get Token", buildIssueTab());
        tabs.addTab("My Tokens / Track", buildTrackTab());
        add(tabs, BorderLayout.CENTER);

        serviceBox.addActionListener(e -> updateLiveInfo());
        timer = new Timer(2000, e -> refresh());
        timer.start();
        refresh();
    }

    // ------------------------------------------------------------- Get Token
    private JPanel buildIssueTab() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(UI.BG);
        JPanel card = UI.card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0;
        JLabel h = new JLabel("Select a service to join its queue");
        h.setFont(new Font("SansSerif", Font.BOLD, 17));
        g.gridwidth = 2;
        card.add(h, g);
        g.gridwidth = 1; g.gridy = 1;
        card.add(new JLabel("Service"), g);
        g.gridx = 1;
        serviceBox.setPreferredSize(new Dimension(260, 30));
        card.add(serviceBox, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        JLabel note = new JLabel("Priority (senior citizen / differently-abled / emergency) is decided by the staff at the counter.");
        note.setFont(new Font("SansSerif", Font.PLAIN, 12));
        note.setForeground(Color.DARK_GRAY);
        card.add(note, g);
        g.gridy = 3;
        liveInfo.setFont(UI.BOLD);
        liveInfo.setForeground(UI.TEAL);
        card.add(liveInfo, g);
        g.gridy = 4;
        JButton get = UI.button("Generate Token", UI.NAVY);
        card.add(get, g);
        get.addActionListener(e -> getToken());
        wrap.add(card);
        return wrap;
    }

    private void getToken() {
        ServiceType s = (ServiceType) serviceBox.getSelectedItem();
        try {
            Token t = svc.issueToken(user.id, user.name, s, false, false);
            refresh();
            tabs.setSelectedIndex(1);
            selectToken(t.label());
            UI.info(this, "Your token number is  " + t.label() + "\n\nService : " + s.label
                    + "\nPosition : " + svc.position(t) + " in queue"
                    + "\nEstimated wait : about " + svc.estimatedWait(t) + " minutes\n\nYou will be alerted when your token is called.");
        } catch (IllegalStateException ex) {
            UI.error(this, ex.getMessage());
        }
    }

    private void updateLiveInfo() {
        ServiceType s = (ServiceType) serviceBox.getSelectedItem();
        if (s == null) return;
        liveInfo.setText("Currently waiting: " + svc.waitingList(s).size()
                + "   |   Estimated wait if you join now: about " + svc.estimatedWaitIfJoined(s) + " min");
    }

    // ------------------------------------------------------------ Track tab
    private JPanel buildTrackTab() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(UI.BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        banner.setFont(new Font("SansSerif", Font.BOLD, 17));
        banner.setOpaque(true);
        banner.setBackground(new Color(0xE3F3F1));
        banner.setForeground(new Color(0x0A5E59));
        banner.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        p.add(banner, BorderLayout.NORTH);

        UI.styleTable(table, 3);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showDetail(); });
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(700, 190));

        JScrollPane dp = new JScrollPane(detail);
        dp.setBorder(BorderFactory.createTitledBorder("Token details"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sp, dp);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        p.add(split, BorderLayout.CENTER);

        JButton cancel = UI.button("Cancel Selected Token", UI.RED);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT));
        south.setOpaque(false);
        south.add(cancel);
        p.add(south, BorderLayout.SOUTH);
        cancel.addActionListener(e -> cancelSelected());
        return p;
    }

    private String selectedLabel() {
        int r = table.getSelectedRow();
        return r < 0 ? null : (String) model.getValueAt(r, 0) + "|" + model.getValueAt(r, 7);
    }

    private Token tokenFor(String key) {
        if (key == null) return null;
        for (Token t : svc.tokensOfStudent(user.id))
            if ((t.label() + "|" + t.issuedAt).equals(key)) return t;
        return null;
    }

    private void selectToken(String label) {
        for (int r = 0; r < model.getRowCount(); r++)
            if (label.equals(model.getValueAt(r, 0))) { table.setRowSelectionInterval(r, r); return; }
    }

    private void showDetail() {
        Token t = tokenFor(selectedLabel());
        detail.setText(t == null ? "Select a token to see its details and status." : svc.describe(t));
        detail.setCaretPosition(0);
    }

    private void cancelSelected() {
        Token t = tokenFor(selectedLabel());
        if (t == null) { UI.error(this, "Select a token first."); return; }
        try {
            svc.cancel(t);
            refresh();
        } catch (IllegalStateException ex) {
            UI.error(this, ex.getMessage());
        }
    }

    // --------------------------------------------------------------- refresh
    private void refresh() {
        String keep = selectedLabel();
        List<Token> mine = svc.tokensOfStudent(user.id);
        model.setRowCount(0);
        Token active = null;
        for (Token t : mine) {
            int pos = svc.position(t);
            model.addRow(new Object[]{t.label(), t.service.label, t.priority ? "PRIORITY" : "NORMAL", t.status.name(),
                    pos > 0 ? String.valueOf(pos) : (pos == 0 ? "Now" : "-"),
                    t.status == Token.Status.WAITING ? "~" + svc.estimatedWait(t) + " min" : "-",
                    t.counter, t.issuedAt});
            if (t.isActive() && active == null) active = t;
            if (t.status == Token.Status.SERVING && notified.add(t.label() + t.issuedAt)) {
                final Token called = t;
                SwingUtilities.invokeLater(() -> UI.info(this, "Your token " + called.label()
                        + " is being called!\n\nPlease go to  " + called.counter + "  (" + called.service.label + ")."));
            }
        }
        if (keep != null)
            for (int r = 0; r < model.getRowCount(); r++)
                if (keep.equals(model.getValueAt(r, 0) + "|" + model.getValueAt(r, 7))) { table.setRowSelectionInterval(r, r); break; }
        if (active == null) {
            banner.setText("You have no active token. Go to 'Get Token' to join a queue.");
        } else if (active.status == Token.Status.SERVING) {
            banner.setText("Token " + active.label() + " - NOW SERVING at " + active.counter + ". Please proceed!");
        } else {
            banner.setText("Token " + active.label() + " (" + active.service.label + ")  |  Position "
                    + svc.position(active) + "  |  Est. wait ~" + svc.estimatedWait(active) + " min");
        }
        showDetail();
        updateLiveInfo();
    }
}
