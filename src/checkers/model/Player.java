package checkers.model;

public class Player {

    private final PieceColor color;
    private final String name;

    public Player(PieceColor color, String name) {
        this.color = color;
        this.name = name;
    }

    public PieceColor getColor() {
        return color;
    }

    public String getName() {
        return name;
    }
}
