package view;

import controller.UserController;
import model.User;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private UserController userController;

    public LoginFrame() {
        super("Login/Register");
        userController = new UserController();
        initUI();
    }

    private void initUI() {
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");

        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        loginButton = new JButton("Login");
        registerButton = new JButton("Register");
        panel.add(loginButton);
        panel.add(registerButton);

        add(panel);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());

        setVisible(true);
    }

    private void login() {
        String user = usernameField.getText();
        String pw = new String(passwordField.getPassword());
        User u = userController.login(user, pw);
        if (u != null) {
            JOptionPane.showMessageDialog(this, "Login success","Message", JOptionPane.INFORMATION_MESSAGE);
            new MainFrame(u).setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password","Message", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void register() {
        String user = usernameField.getText();
        String pw = new String(passwordField.getPassword());
        if (userController.register(user, pw)) {
            JOptionPane.showMessageDialog(this, "Registration success, please login","Message", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Username already exists","Message", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
