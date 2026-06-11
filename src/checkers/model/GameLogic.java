package checkers.model;

import java.util.ArrayList;
import java.util.List;

public class GameLogic {

    private PieceColor currentTurn;
    private Integer continuationX;
    private Integer continuationY;

    public GameLogic() {
        this.currentTurn = PieceColor.WHITE;
    }

    public PieceColor getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(PieceColor currentTurn) {
        this.currentTurn = currentTurn;
        clearContinuation();
    }

    public boolean isContinuationRequired() {
        return continuationX != null;
    }

    public int getContinuationX() {
        return continuationX;
    }

    public int getContinuationY() {
        return continuationY;
    }

    public void clearContinuation() {
        continuationX = null;
        continuationY = null;
    }

    public List<Move> getLegalMoves(Board board, int fromX, int fromY) {
        Piece piece = board.getPiece(fromX, fromY);
        if (piece == null || piece.getColor() != currentTurn) {
            return List.of();
        }
        if (isContinuationRequired()) {
            if (fromX != continuationX || fromY != continuationY) {
                return List.of();
            }
            return getCaptureMovesFrom(board, fromX, fromY, piece);
        }
        boolean mustCapture = hasCaptureMoves(board, currentTurn);
        if (mustCapture) {
            List<Move> captures = getCaptureMovesFrom(board, fromX, fromY, piece);
            return captures.isEmpty() ? List.of() : captures;
        }
        List<Move> moves = new ArrayList<>();
        moves.addAll(getSimpleMovesFrom(board, fromX, fromY, piece));
        moves.addAll(getCaptureMovesFrom(board, fromX, fromY, piece));
        return moves;
    }

    public List<Move> getAllLegalMoves(Board board, PieceColor color) {
        List<Move> moves = new ArrayList<>();
        if (color != currentTurn) {
            return moves;
        }
        if (isContinuationRequired()) {
            moves.addAll(getLegalMoves(board, continuationX, continuationY));
            return moves;
        }
        boolean mustCapture = hasCaptureMoves(board, color);
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.getPiece(col, row);
                if (piece != null && piece.getColor() == color) {
                    if (mustCapture) {
                        moves.addAll(getCaptureMovesFrom(board, col, row, piece));
                    } else {
                        moves.addAll(getSimpleMovesFrom(board, col, row, piece));
                        moves.addAll(getCaptureMovesFrom(board, col, row, piece));
                    }
                }
            }
        }
        return moves;
    }

    public boolean isValidMove(Board board, Move move) {
        List<Move> legalMoves = getLegalMoves(board, move.getFromX(), move.getFromY());
        for (Move legal : legalMoves) {
            if (legal.getToX() == move.getToX() && legal.getToY() == move.getToY()) {
                if (!legal.isCapture() && !move.isCapture()) {
                    return true;
                }
                if (legal.isCapture() && move.isCapture()
                        && legal.getCapturedX() == move.getCapturedX()
                        && legal.getCapturedY() == move.getCapturedY()) {
                    return true;
                }
            }
        }
        return false;
    }

    public MoveResult applyMove(Board board, Move move) {
        if (!isValidMove(board, move)) {
            return MoveResult.failure();
        }
        Piece piece = board.getPiece(move.getFromX(), move.getFromY());
        board.movePiece(move.getFromX(), move.getFromY(), move.getToX(), move.getToY());
        if (move.isCapture()) {
            board.removePiece(move.getCapturedX(), move.getCapturedY());
        }
        boolean promoted = promoteIfNeeded(board, move.getToX(), move.getToY(), piece);
        Piece movedPiece = board.getPiece(move.getToX(), move.getToY());
        if (move.isCapture() && !promoted && hasCaptureFrom(board, move.getToX(), move.getToY(), movedPiece)) {
            continuationX = move.getToX();
            continuationY = move.getToY();
            return new MoveResult(true, promoted, true, currentTurn);
        }
        clearContinuation();
        PieceColor nextTurn = currentTurn.opposite();
        currentTurn = nextTurn;
        return new MoveResult(true, promoted, false, nextTurn);
    }

    public PieceColor checkWinner(Board board) {
        if (board.countPieces(PieceColor.WHITE) == 0) {
            return PieceColor.BLACK;
        }
        if (board.countPieces(PieceColor.BLACK) == 0) {
            return PieceColor.WHITE;
        }
        if (getAllLegalMoves(board, currentTurn).isEmpty()) {
            return currentTurn.opposite();
        }
        return null;
    }

    private boolean promoteIfNeeded(Board board, int x, int y, Piece piece) {
        if (piece.isKing()) {
            return false;
        }
        if (piece.getColor() == PieceColor.WHITE && y == 0) {
            piece.promoteToKing();
            return true;
        }
        if (piece.getColor() == PieceColor.BLACK && y == Board.SIZE - 1) {
            piece.promoteToKing();
            return true;
        }
        return false;
    }

    private boolean hasCaptureMoves(Board board, PieceColor color) {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Piece piece = board.getPiece(col, row);
                if (piece != null && piece.getColor() == color) {
                    if (!getCaptureMovesFrom(board, col, row, piece).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasCaptureFrom(Board board, int x, int y, Piece piece) {
        return !getCaptureMovesFrom(board, x, y, piece).isEmpty();
    }

    private List<Move> getSimpleMovesFrom(Board board, int x, int y, Piece piece) {
        List<Move> moves = new ArrayList<>();
        int[][] directions = getMoveDirections(piece, false);
        for (int[] direction : directions) {
            int targetX = x + direction[0];
            int targetY = y + direction[1];
            if (Board.isPlayableSquare(targetX, targetY) && board.getPiece(targetX, targetY) == null) {
                moves.add(new Move(x, y, targetX, targetY));
            }
        }
        return moves;
    }

    private List<Move> getCaptureMovesFrom(Board board, int x, int y, Piece piece) {
        List<Move> moves = new ArrayList<>();
        int[][] directions = getMoveDirections(piece, true);
        for (int[] direction : directions) {
            int jumpedX = x + direction[0];
            int jumpedY = y + direction[1];
            int landingX = x + direction[0] * 2;
            int landingY = y + direction[1] * 2;
            if (!Board.isPlayableSquare(landingX, landingY)) {
                continue;
            }
            Piece jumpedPiece = board.getPiece(jumpedX, jumpedY);
            if (jumpedPiece != null
                    && jumpedPiece.getColor() != piece.getColor()
                    && board.getPiece(landingX, landingY) == null) {
                moves.add(new Move(x, y, landingX, landingY, jumpedX, jumpedY));
            }
        }
        return moves;
    }

    private int[][] getMoveDirections(Piece piece, boolean forCapture) {
        if (piece.isKing()) {
            return new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        }
        if (piece.getColor() == PieceColor.WHITE) {
            return forCapture
                    ? new int[][]{{1, -1}, {-1, -1}, {1, 1}, {-1, 1}}
                    : new int[][]{{1, -1}, {-1, -1}};
        }
        return forCapture
                ? new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}
                : new int[][]{{1, 1}, {-1, 1}};
    }
}
