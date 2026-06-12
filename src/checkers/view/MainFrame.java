package checkers.view;

import checkers.model.Board;
import checkers.model.PieceColor;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {

    private final BoardPanel boardPanel;
    private final ChatPanel chatPanel;
    private final JLabel statusLabel;
    private final JButton restartButton;

    public MainFrame() {
        super("Network Checkers");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false); 
        setSize(980, 519);

        chatPanel = new ChatPanel();
        statusLabel = new JLabel("Waiting for connection...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        restartButton = new JButton("Restart Game");
        restartButton.setEnabled(false);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(restartButton, BorderLayout.EAST);

        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(chatPanel, BorderLayout.CENTER);
        sidePanel.add(bottomPanel, BorderLayout.SOUTH);

        boardPanel = new BoardPanel(chatPanel);

        add(boardPanel, BorderLayout.WEST);
        add(sidePanel, BorderLayout.EAST);

        setLocationRelativeTo(null);
    }

    public void setRestartAction(ActionListener action) {
        restartButton.addActionListener(action);
    }

    public void setRestartButtonEnabled(boolean enabled) {
        restartButton.setEnabled(enabled);
    }

    public BoardPanel getBoardPanel() {
        return boardPanel;
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    public void initializeBoard(Board board) {
        boardPanel.setBoard(board);
    }

    public void updateTurnStatus(PieceColor localColor, PieceColor currentTurn, boolean gameOver) {
        if (gameOver) {
            return;
        }
        if (currentTurn == localColor) {
            setStatusText("Your turn (" + formatColor(localColor) + ")");
        } else {
            setStatusText("Opponent's turn (" + formatColor(currentTurn) + ")");
        }
    }

    public void showGameOver(PieceColor winner, PieceColor localColor) {
        if (winner == localColor) {
            setStatusText("You win!");
        } else if (winner != null) {
            setStatusText("You lose.");
        } else {
            setStatusText("Game ended.");
        }
    }

    private String formatColor(PieceColor color) {
        return color == PieceColor.WHITE ? "White" : "Black";
    }
}
