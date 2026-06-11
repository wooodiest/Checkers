package checkers.model;

public class Board {

    public static final int SIZE = 8;

    private final Piece[][] grid;

    public Board() {
        grid = new Piece[SIZE][SIZE];
        setupInitialPieces();
    }

    private Board(Piece[][] grid) {
        this.grid = grid;
    }

    private void setupInitialPieces() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (isPlayableSquare(col, row)) {
                    grid[col][row] = new Piece(PieceColor.BLACK);
                }
            }
        }
        for (int row = 5; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (isPlayableSquare(col, row)) {
                    grid[col][row] = new Piece(PieceColor.WHITE);
                }
            }
        }
    }

    public static boolean isPlayableSquare(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && (x + y) % 2 == 1;
    }

    public Piece getPiece(int x, int y) {
        if (!isInside(x, y)) {
            return null;
        }
        return grid[x][y];
    }

    public void setPiece(int x, int y, Piece piece) {
        if (isInside(x, y)) {
            grid[x][y] = piece;
        }
    }

    public void movePiece(int fromX, int fromY, int toX, int toY) {
        grid[toX][toY] = grid[fromX][fromY];
        grid[fromX][fromY] = null;
    }

    public void removePiece(int x, int y) {
        if (isInside(x, y)) {
            grid[x][y] = null;
        }
    }

    public int countPieces(PieceColor color) {
        int count = 0;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Piece piece = grid[col][row];
                if (piece != null && piece.getColor() == color) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    public Board copy() {
        Piece[][] copy = new Piece[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Piece piece = grid[col][row];
                copy[col][row] = piece == null ? null : piece.copy();
            }
        }
        return new Board(copy);
    }
}
