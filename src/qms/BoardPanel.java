package qms;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;

/** Content of the public "Now Serving" board: one card per service, refreshed by the owner. */
public class BoardPanel extends JPanel {
    private static final Color DARK = new Color(0x0F1630);
    private final QueueService svc;
    private final Map<ServiceType, JLabel> serving = new EnumMap<>(ServiceType.class);
    private final Map<ServiceType, JLabel> info = new EnumMap<>(ServiceType.class);
    private final JLabel clock = new JLabel();
    private final Set<String> lastServing = new HashSet<>();

    public BoardPanel(QueueService svc) {
        this.svc = svc;
        setLayout(new BorderLayout());
        add(buildContent());
        refresh();
    }

    /** The board content . */
    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(DARK);
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("NOW SERVING");
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        title.setForeground(Color.WHITE);
        clock.setFont(new Font("SansSerif", Font.BOLD, 26));
        clock.setForeground(new Color(0xFFC857));
        JLabel college = new JLabel("K. Ramakrishnan College of Technology - College Services");
        college.setFont(new Font("SansSerif", Font.PLAIN, 14));
        college.setForeground(new Color(0xB8C2E0));
        JPanel l = new JPanel(new GridLayout(2, 1));
        l.setOpaque(false);
        l.add(title);
        l.add(college);
        head.add(l, BorderLayout.WEST);
        head.add(clock, BorderLayout.EAST);
        root.add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 14, 14));
        grid.setOpaque(false);
        for (ServiceType s : ServiceType.values()) {
            JPanel c = new JPanel(new BorderLayout(0, 6));
            c.setBackground(new Color(0x1B2750));
            c.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            JLabel name = new JLabel(s.label.toUpperCase());
            name.setFont(new Font("SansSerif", Font.BOLD, 17));
            name.setForeground(new Color(0x8FD8D2));
            JLabel big = new JLabel("-", SwingConstants.CENTER);
            big.setFont(new Font("SansSerif", Font.BOLD, 40));
            big.setForeground(Color.WHITE);
            JLabel sub = new JLabel(" ", SwingConstants.CENTER);
            sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
            sub.setForeground(new Color(0xB8C2E0));
            c.add(name, BorderLayout.NORTH);
            c.add(big, BorderLayout.CENTER);
            c.add(sub, BorderLayout.SOUTH);
            grid.add(c);
            serving.put(s, big);
            info.put(s, sub);
        }
        JPanel legend = new JPanel(new GridBagLayout());
        legend.setBackground(new Color(0x1B2750));
        JLabel lg = new JLabel("<html><center>Please listen for your token.<br>Priority tokens are served first.</center></html>",
                SwingConstants.CENTER);
        lg.setFont(new Font("SansSerif", Font.PLAIN, 16));
        lg.setForeground(new Color(0xFFC857));
        legend.add(lg);
        grid.add(legend);
        root.add(grid, BorderLayout.CENTER);
        return root;
    }

    public void refresh() {
        clock.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
        Set<String> now = new HashSet<>();
        for (ServiceType s : ServiceType.values()) {
            List<Token> sv = svc.servingList(s);
            List<Token> w = svc.waitingList(s);
            StringBuilder sb = new StringBuilder();
            for (Token t : sv) {
                if (sb.length() > 0) sb.append("<br>");
                sb.append(t.label()).append(" > C").append(t.counter.replace("Counter ", ""));
                now.add(t.label() + t.issuedAt);
            }
            serving.get(s).setText(sb.length() == 0 ? "-" : "<html><center>" + sb + "</center></html>");
            serving.get(s).setFont(new Font("SansSerif", Font.BOLD, sv.size() > 1 ? 26 : 40));
            info.get(s).setText("Waiting: " + w.size() + (w.isEmpty() ? "" : "   |   Next: " + w.get(0).label()));
        }
        if (!lastServing.containsAll(now)) Toolkit.getDefaultToolkit().beep();
        lastServing.clear();
        lastServing.addAll(now);
    }
}
