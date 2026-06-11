package checkers.model;

public class Move {

    private final int fromX;
    private final int fromY;
    private final int toX;
    private final int toY;
    private final int capturedX;
    private final int capturedY;

    public Move(int fromX, int fromY, int toX, int toY) {
        this(fromX, fromY, toX, toY, -1, -1);
    }

    public Move(int fromX, int fromY, int toX, int toY, int capturedX, int capturedY) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.capturedX = capturedX;
        this.capturedY = capturedY;
    }

    public int getFromX() {
        return fromX;
    }

    public int getFromY() {
        return fromY;
    }

    public int getToX() {
        return toX;
    }

    public int getToY() {
        return toY;
    }

    public boolean isCapture() {
        return capturedX >= 0;
    }

    public int getCapturedX() {
        return capturedX;
    }

    public int getCapturedY() {
        return capturedY;
    }
}
