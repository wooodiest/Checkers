package checkers.model;

public class Piece {

    private final PieceColor color;
    private boolean king;

    public Piece(PieceColor color) {
        this.color = color;
    }

    public PieceColor getColor() {
        return color;
    }

    public boolean isKing() {
        return king;
    }

    public void promoteToKing() {
        king = true;
    }

    public Piece copy() {
        Piece copy = new Piece(color);
        copy.king = king;
        return copy;
    }
}
