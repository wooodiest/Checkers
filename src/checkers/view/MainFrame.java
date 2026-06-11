package checkers.view;

import checkers.model.Board;
import checkers.model.PieceColor;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;

public class MainFrame extends JFrame {

    private final BoardPanel boardPanel;
    private final ChatPanel chatPanel;
    private final JLabel statusLabel;

    public MainFrame() {
        super("Network Checkers");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        boardPanel = new BoardPanel();
        chatPanel = new ChatPanel();
        statusLabel = new JLabel("Waiting for connection...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(chatPanel, BorderLayout.CENTER);
        sidePanel.add(statusLabel, BorderLayout.SOUTH);

        add(boardPanel, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
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
