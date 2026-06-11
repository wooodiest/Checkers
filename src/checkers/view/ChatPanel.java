package checkers.view;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatPanel extends JPanel {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final JTextArea historyArea;
    private final JTextField inputField;

    public ChatPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Chat"));

        historyArea = new JTextArea(20, 50);
        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);

        Font font = historyArea.getFont().deriveFont(Font.PLAIN, 15f);
        historyArea.setFont(font);

        inputField = new JTextField();
        inputField.setFont(font);

        add(new JScrollPane(historyArea), BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);
    }

    public void setSendAction(ActionListener listener) {
        inputField.addActionListener(event -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                listener.actionPerformed(event);
                inputField.setText("");
            }
        });
    }

    public String getInputText() {
        return inputField.getText().trim();
    }

    public void clearInput() {
        inputField.setText("");
    }

    public void appendMessage(String sender, String message) {
        String timestamp = LocalTime.now().format(TIME_FORMAT);
        historyArea.append("[" + timestamp + "] " + sender + ": " + message + System.lineSeparator());
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }

    public void appendSystemMessage(String message) {
        historyArea.append(message + System.lineSeparator());
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }
}
