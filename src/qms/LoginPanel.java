package qms;

import java.awt.*;
import javax.swing.*;

/** Login / registration screen. */
public class LoginPanel extends JPanel {
    private final QueueService svc;
    private final Nav nav;
    private final JTextField idField = new JTextField(18);
    private final JPasswordField pwField = new JPasswordField(18);

    public LoginPanel(QueueService svc, Nav nav) {
        this.svc = svc;
        this.nav = nav;
        setLayout(new BorderLayout());
        setBackground(UI.BG);
        add(UI.header("Queue Management System", "K. Ramakrishnan College of Technology (Autonomous)", null),
                BorderLayout.NORTH);

        JPanel form = UI.card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        JLabel title = new JLabel("Login to continue");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        form.add(title, g);
        g.gridwidth = 1; g.gridy = 1;
        form.add(new JLabel("Register No. / Staff ID"), g);
        g.gridx = 1;
        form.add(idField, g);
        g.gridx = 0; g.gridy = 2;
        form.add(new JLabel("Password"), g);
        g.gridx = 1;
        form.add(pwField, g);

        JButton login = UI.button("Login", UI.NAVY);
        JButton register = UI.button("New Student? Register", UI.TEAL);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        btns.add(login);
        btns.add(register);
        g.gridx = 1; g.gridy = 3;
        form.add(btns, g);

        JButton board = UI.button("Open Live Display Board", UI.AMBER);
        g.gridx = 1; g.gridy = 4;
        form.add(board, g);

        JLabel hint = new JLabel("Staff demo login:  admin / admin123");
        hint.setForeground(Color.DARK_GRAY);
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2;
        form.add(hint, g);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(form);
        add(center, BorderLayout.CENTER);

        login.addActionListener(e -> doLogin());
        pwField.addActionListener(e -> doLogin());
        idField.addActionListener(e -> pwField.requestFocus());
        register.addActionListener(e -> doRegister());
        board.addActionListener(e -> nav.openBoard());
    }

    private void doLogin() {
        User u = svc.login(idField.getText(), new String(pwField.getPassword()));
        if (u == null) {
            UI.error(this, "Invalid ID or password.");
            return;
        }
        pwField.setText("");
        if (u.role == User.Role.STAFF) nav.showStaff(u); else nav.showStudent(u);
    }

    private void doRegister() {
        JTextField reg = new JTextField(), name = new JTextField();
        JPasswordField p1 = new JPasswordField(), p2 = new JPasswordField();
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 8));
        p.add(new JLabel("Register No.")); p.add(reg);
        p.add(new JLabel("Full name")); p.add(name);
        p.add(new JLabel("Password")); p.add(p1);
        p.add(new JLabel("Confirm password")); p.add(p2);
        if (JOptionPane.showConfirmDialog(this, p, "Student Registration", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        if (!new String(p1.getPassword()).equals(new String(p2.getPassword()))) {
            UI.error(this, "Passwords do not match.");
            return;
        }
        try {
            svc.registerStudent(reg.getText(), name.getText(), new String(p1.getPassword()));
            idField.setText(reg.getText().trim());
            UI.info(this, "Registration successful. You can log in now.");
        } catch (IllegalArgumentException ex) {
            UI.error(this, ex.getMessage());
        }
    }
}
