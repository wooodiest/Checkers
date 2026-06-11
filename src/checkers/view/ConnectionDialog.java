package checkers.view;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Frame;

public class ConnectionDialog extends JDialog {

    public enum ConnectionMode {
        HOST,
        JOIN
    }

    public static class ConnectionConfig {
        private final ConnectionMode mode;
        private final String host;
        private final int port;
        private final boolean confirmed;

        public ConnectionConfig(ConnectionMode mode, String host, int port, boolean confirmed) {
            this.mode = mode;
            this.host = host;
            this.port = port;
            this.confirmed = confirmed;
        }

        public ConnectionMode getMode() {
            return mode;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public boolean isConfirmed() {
            return confirmed;
        }
    }

    private final JRadioButton hostButton;
    private final JRadioButton joinButton;
    private final JTextField hostField;
    private final JTextField portField;
    private ConnectionConfig result;

    public ConnectionDialog(Frame owner) {
        super(owner, "Network Checkers", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        hostButton = new JRadioButton("Host server", true);
        joinButton = new JRadioButton("Join game");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(hostButton);
        modeGroup.add(joinButton);

        hostField = new JTextField("127.0.0.1", 20);
        hostField.setEnabled(false);
        portField = new JTextField("5000", 20);

        hostButton.addActionListener(event -> hostField.setEnabled(false));
        joinButton.addActionListener(event -> hostField.setEnabled(true));

        JPanel modePanel = new JPanel(new GridLayout(2, 1, 5, 5));
        modePanel.setBorder(BorderFactory.createTitledBorder("Connection mode"));
        modePanel.add(hostButton);
        modePanel.add(joinButton);

        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        fieldsPanel.add(new JLabel("Host address:"));
        fieldsPanel.add(hostField);
        fieldsPanel.add(new JLabel("Port:"));
        fieldsPanel.add(portField);

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(event -> confirm());

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        centerPanel.add(modePanel, BorderLayout.NORTH);
        centerPanel.add(fieldsPanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
        add(connectButton, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private void confirm() {
        ConnectionMode mode = hostButton.isSelected() ? ConnectionMode.HOST : ConnectionMode.JOIN;
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException exception) {
            port = 5000;
        }
        result = new ConnectionConfig(mode, host, port, true);
        dispose();
    }

    public ConnectionConfig showDialog() {
        setVisible(true);
        if (result == null) {
            return new ConnectionConfig(ConnectionMode.HOST, "127.0.0.1", 5000, false);
        }
        return result;
    }
}
