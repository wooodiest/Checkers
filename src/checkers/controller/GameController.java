package checkers.controller;

import checkers.model.Board;
import checkers.model.GameLogic;
import checkers.model.Move;
import checkers.model.PieceColor;
import checkers.model.Player;
import checkers.view.MainFrame;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.util.List;

public class GameController implements NetworkListener {

    private final MainFrame mainFrame;
    private final NetworkManager networkManager;

    private Board board;
    private GameLogic gameLogic;
    private Player localPlayer;
    private Point selectedSquare;
    private boolean gameStarted;
    private boolean gameOver;

    public GameController(MainFrame mainFrame, NetworkManager networkManager) {
        this.mainFrame = mainFrame;
        this.networkManager = networkManager;
        this.board = new Board();
        this.gameLogic = new GameLogic();

        mainFrame.initializeBoard(board);
        mainFrame.getBoardPanel().setSquareClickListener(this::handleSquareClick);
        mainFrame.getChatPanel().setSendAction(event -> sendChatMessage());
    }

    public void startNetwork() {
        networkManager.start();
    }

    @Override
    public void onConnected(PieceColor assignedColor) {
        SwingUtilities.invokeLater(() -> {
            localPlayer = new Player(assignedColor, "You");
            gameStarted = true;
            mainFrame.getChatPanel().appendSystemMessage("Connected. You play as " + formatColor(assignedColor) + ".");
            refreshView();
        });
    }

    @Override
    public void onChatMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> mainFrame.getChatPanel().appendMessage(sender, message));
    }

    @Override
    public void onRemoteMove(int fromX, int fromY, int toX, int toY) {
        SwingUtilities.invokeLater(() -> applyRemoteMove(fromX, fromY, toX, toY));
    }

    @Override
    public void onGameEnd(String reason) {
        SwingUtilities.invokeLater(() -> handleRemoteGameEnd(reason));
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            if (!gameOver) {
                gameOver = true;
                mainFrame.getChatPanel().appendSystemMessage("Connection lost.");
                mainFrame.setStatusText("Disconnected");
            }
        });
    }

    private void handleSquareClick(int x, int y) {
        if (!gameStarted || gameOver || localPlayer == null) {
            return;
        }
        if (gameLogic.getCurrentTurn() != localPlayer.getColor()) {
            return;
        }
        if (gameLogic.isContinuationRequired()) {
            int continuationX = gameLogic.getContinuationX();
            int continuationY = gameLogic.getContinuationY();
            if (x == continuationX && y == continuationY) {
                selectSquare(x, y);
                return;
            }
            attemptMove(continuationX, continuationY, x, y);
            return;
        }
        if (selectedSquare == null) {
            selectSquare(x, y);
            return;
        }
        if (selectedSquare.x == x && selectedSquare.y == y) {
            clearSelection();
            return;
        }
        if (board.getPiece(x, y) != null && board.getPiece(x, y).getColor() == localPlayer.getColor()) {
            selectSquare(x, y);
            return;
        }
        attemptMove(selectedSquare.x, selectedSquare.y, x, y);
    }

    private void selectSquare(int x, int y) {
        List<Move> legalMoves = gameLogic.getLegalMoves(board, x, y);
        if (legalMoves.isEmpty()) {
            clearSelection();
            return;
        }
        selectedSquare = new Point(x, y);
        mainFrame.getBoardPanel().setSelectedSquare(x, y);
        mainFrame.getBoardPanel().setHighlightedTargets(legalMoves);
    }

    private void clearSelection() {
        selectedSquare = null;
        mainFrame.getBoardPanel().clearSelection();
    }

    private void attemptMove(int fromX, int fromY, int toX, int toY) {
        Move move = resolveMove(fromX, fromY, toX, toY);
        if (move == null) {
            if (board.getPiece(toX, toY) != null && board.getPiece(toX, toY).getColor() == localPlayer.getColor()) {
                selectSquare(toX, toY);
            }
            return;
        }
        executeLocalMove(move, true);
    }

    private Move resolveMove(int fromX, int fromY, int toX, int toY) {
        List<Move> legalMoves = gameLogic.getLegalMoves(board, fromX, fromY);
        for (Move legalMove : legalMoves) {
            if (legalMove.getToX() == toX && legalMove.getToY() == toY) {
                return legalMove;
            }
        }
        return null;
    }

    private void applyRemoteMove(int fromX, int fromY, int toX, int toY) {
        Move move = resolveMove(fromX, fromY, toX, toY);
        if (move == null) {
            move = buildFallbackMove(fromX, fromY, toX, toY);
        }
        executeLocalMove(move, false);
    }

    private Move buildFallbackMove(int fromX, int fromY, int toX, int toY) {
        int deltaX = toX - fromX;
        int deltaY = toY - fromY;
        if (Math.abs(deltaX) == 2 && Math.abs(deltaY) == 2) {
            int capturedX = fromX + deltaX / 2;
            int capturedY = fromY + deltaY / 2;
            return new Move(fromX, fromY, toX, toY, capturedX, capturedY);
        }
        return new Move(fromX, fromY, toX, toY);
    }

    private void executeLocalMove(Move move, boolean sendToNetwork) {
        MoveResultWrapper result = applyMoveToModel(move);
        if (!result.success()) {
            return;
        }
        if (sendToNetwork) {
            networkManager.sendMove(move.getFromX(), move.getFromY(), move.getToX(), move.getToY());
        }
        if (result.continuesCapture()) {
            selectedSquare = new Point(move.getToX(), move.getToY());
            List<Move> followUpMoves = gameLogic.getLegalMoves(board, move.getToX(), move.getToY());
            mainFrame.getBoardPanel().setSelectedSquare(move.getToX(), move.getToY());
            mainFrame.getBoardPanel().setHighlightedTargets(followUpMoves);
        } else {
            clearSelection();
        }
        refreshView();
        checkForWinner(sendToNetwork);
    }

    private MoveResultWrapper applyMoveToModel(Move move) {
        var result = gameLogic.applyMove(board, move);
        return new MoveResultWrapper(result.isSuccess(), result.isContinuesCapture());
    }

    private void checkForWinner(boolean notifyOpponent) {
        PieceColor winner = gameLogic.checkWinner(board);
        if (winner == null) {
            return;
        }
        gameOver = true;
        mainFrame.showGameOver(winner, localPlayer.getColor());
        mainFrame.getChatPanel().appendSystemMessage(formatColor(winner) + " wins.");
        if (notifyOpponent) {
            networkManager.sendGameEnd(winner.name() + "_WINS");
        }
    }

    private void handleRemoteGameEnd(String reason) {
        gameOver = true;
        if ("DISCONNECTED".equals(reason)) {
            mainFrame.getChatPanel().appendSystemMessage("Opponent disconnected.");
            mainFrame.setStatusText("Disconnected");
            return;
        }
        if (reason.endsWith("_WINS")) {
            String winnerName = reason.substring(0, reason.length() - "_WINS".length());
            PieceColor winner = PieceColor.valueOf(winnerName);
            mainFrame.showGameOver(winner, localPlayer.getColor());
            mainFrame.getChatPanel().appendSystemMessage(formatColor(winner) + " wins.");
        } else {
            mainFrame.getChatPanel().appendSystemMessage("Game ended: " + reason);
            mainFrame.setStatusText("Game ended");
        }
    }

    private void sendChatMessage() {
        String text = mainFrame.getChatPanel().getInputText();
        if (text.isEmpty() || !gameStarted) {
            return;
        }
        mainFrame.getChatPanel().appendMessage("You", text);
        mainFrame.getChatPanel().clearInput();
        networkManager.sendChat(text);
    }

    private void refreshView() {
        mainFrame.getBoardPanel().setBoard(board);
        if (localPlayer != null) {
            mainFrame.updateTurnStatus(localPlayer.getColor(), gameLogic.getCurrentTurn(), gameOver);
        }
    }

    private String formatColor(PieceColor color) {
        return color == PieceColor.WHITE ? "White" : "Black";
    }

    public void shutdown() {
        if (!gameOver) {
            networkManager.sendGameEnd("DISCONNECTED");
        } else {
            networkManager.shutdown();
        }
    }

    private record MoveResultWrapper(boolean success, boolean continuesCapture) {
    }
}
