package checkers.controller;

import checkers.model.Board;
import checkers.model.GameLogic;
import checkers.model.Move;
import checkers.model.Piece;
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
    private PieceColor assignedColor;

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
            this.assignedColor = assignedColor;
            localPlayer = new Player(assignedColor, "You");
            gameStarted = true;
            mainFrame.setRestartButtonEnabled(true);
            appendTimedSystemMessage("Connected. You play as " + formatColor(assignedColor) + ".");
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
                appendTimedSystemMessage("Connection lost.");
                mainFrame.setStatusText("Disconnected");
            }
        });
    }

    @Override
    public void onRestartRequest() {
        SwingUtilities.invokeLater(() -> restartGame());
    }

    private void restartGame() {
        board = new Board();
        gameLogic = new GameLogic();
        selectedSquare = null;
        gameOver = false;
        clearSelection();
        mainFrame.initializeBoard(board);
        appendTimedSystemMessage("Game restarted!");
        refreshView();
    }

    public void requestRestart() {
        if (gameStarted) {
            networkManager.sendRestart();
            restartGame();
        }
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
        if (Math.abs(deltaX) == Math.abs(deltaY) && Math.abs(deltaX) >= 2) {
            int stepX = deltaX / Math.abs(deltaX);
            int stepY = deltaY / Math.abs(deltaY);
            int checkX = fromX + stepX;
            int checkY = fromY + stepY;
            Piece capturedPiece = null;
            int capturedX = -1, capturedY = -1;
            while (checkX != toX && checkY != toY) {
                Piece p = board.getPiece(checkX, checkY);
                if (p != null) {
                    if (capturedPiece == null) {
                        capturedPiece = p;
                        capturedX = checkX;
                        capturedY = checkY;
                    } else {
                        capturedPiece = null;
                        break;
                    }
                }
                checkX += stepX;
                checkY += stepY;
            }
            if (capturedPiece != null) {
                return new Move(fromX, fromY, toX, toY, capturedX, capturedY);
            }
        }
        return new Move(fromX, fromY, toX, toY);
    }

    private void executeLocalMove(Move move, boolean sendToNetwork) {
        MoveResultWrapper result = applyMoveToModel(move);
        if (!result.success()) {
            return;
        }

        String playerName = sendToNetwork ? "You" : "Opponent";
        String moveMessage = formatMoveMessage(playerName, move);
        appendTimedSystemMessage(moveMessage);

        if (sendToNetwork) {
            networkManager.sendMove(move.getFromX(), move.getFromY(), move.getToX(), move.getToY());
        }

        PieceColor winner = gameLogic.checkWinner(board);
        if (winner != null) {
            gameOver = true;
            mainFrame.showGameOver(winner, localPlayer.getColor());
            appendTimedSystemMessage(formatColor(winner) + " wins.");
            refreshView(); 
            if (sendToNetwork) {
                networkManager.sendGameEnd(winner.name() + "_WINS");
            }
            return;
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
    }
    
    private void appendTimedSystemMessage(String message) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        mainFrame.getChatPanel().appendSystemMessage("[" + timestamp + "] " + message);
    }
    
    private String formatMoveMessage(String playerName, Move move) {
        String from = "(" + move.getFromX() + ", " + move.getFromY() + ")";
        String to = "(" + move.getToX() + ", " + move.getToY() + ")";
        String capturePart = move.isCapture() ? " (captured a piece)" : "";
        return playerName + " moved from " + from + " to " + to + capturePart + ".";
    }

    private MoveResultWrapper applyMoveToModel(Move move) {
        var result = gameLogic.applyMove(board, move);
        return new MoveResultWrapper(result.isSuccess(), result.isContinuesCapture());
    }



    private void handleRemoteGameEnd(String reason) {
        if (gameOver) {
            return;
        }
        gameOver = true;
        if ("DISCONNECTED".equals(reason)) {
            appendTimedSystemMessage("Opponent disconnected.");
            mainFrame.setStatusText("Disconnected");
            return;
        }
        if (reason.endsWith("_WINS")) {
            String winnerName = reason.substring(0, reason.length() - "_WINS".length());
            PieceColor winner = PieceColor.valueOf(winnerName);
            mainFrame.showGameOver(winner, localPlayer.getColor());
            appendTimedSystemMessage(formatColor(winner) + " wins.");
            refreshView();
        } else {
            appendTimedSystemMessage("Game ended: " + reason);
            mainFrame.setStatusText("Game ended");
            refreshView();
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
        }
        networkManager.shutdown();
    }

    private record MoveResultWrapper(boolean success, boolean continuesCapture) {
    }
}
