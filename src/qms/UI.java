package qms;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/** Shared colours, fonts and small helper factories for the Swing screens. */
final class UI {
    static final Color NAVY = new Color(0x1B2A57);
    static final Color TEAL = new Color(0x0E8C85);
    static final Color RED = new Color(0xC62F4B);
    static final Color AMBER = new Color(0xE08A00);
    static final Color BG = new Color(0xF2F5FA);
    static final Color CARD = Color.WHITE;
    static final Font BASE = new Font("SansSerif", Font.PLAIN, 14);
    static final Font BOLD = new Font("SansSerif", Font.BOLD, 14);

    private UI() { }

    static JPanel header(String title, String subtitle, JComponent right) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(NAVY);
        p.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 20));
        t.setForeground(Color.WHITE);
        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("SansSerif", Font.PLAIN, 12));
        s.setForeground(new Color(0xC5CCE0));
        left.add(t);
        left.add(s);
        p.add(left, BorderLayout.WEST);
        if (right != null) p.add(right, BorderLayout.EAST);
        return p;
    }

    static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setOpaque(true);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(BOLD);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JPanel card(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDE3EE)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        return p;
    }

    static JLabel statCard(String title, JLabel valueOut, Color accent) {
        valueOut.setFont(new Font("SansSerif", Font.BOLD, 26));
        valueOut.setForeground(accent);
        return valueOut;
    }

    static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /** Styles a table and colours the cell text of the given status column. */
    static void styleTable(JTable t, int statusCol) {
        t.setRowHeight(28);
        t.setFont(BASE);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(0xD7E6F7));
        t.setSelectionForeground(Color.BLACK);
        t.setFillsViewportHeight(true);
        JTableHeader h = t.getTableHeader();
        h.setFont(BOLD);
        h.setReorderingAllowed(false);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(tb, v, sel, foc, r, c);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (!sel) comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF6F8FC));
                comp.setForeground(Color.BLACK);
                comp.setFont(BASE);
                if (c == statusCol && v != null) {
                    comp.setFont(BOLD);
                    switch (v.toString()) {
                        case "WAITING": comp.setForeground(AMBER); break;
                        case "SERVING": comp.setForeground(TEAL); break;
                        case "COMPLETED": comp.setForeground(new Color(0x3D5A99)); break;
                        case "SKIPPED": comp.setForeground(RED); break;
                        default: comp.setForeground(Color.GRAY);
                    }
                }
                return comp;
            }
        });
    }

    static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Queue Management System", JOptionPane.WARNING_MESSAGE);
    }

    static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Queue Management System", JOptionPane.INFORMATION_MESSAGE);
    }

    static JTextArea detailArea() {
        JTextArea a = new JTextArea(9, 40);
        a.setEditable(false);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        a.setMargin(new Insets(8, 10, 8, 10));
        return a;
    }
}
