package checkers.model;

public class MoveResult {

    private final boolean success;
    private final boolean promoted;
    private final boolean continuesCapture;
    private final PieceColor nextTurn;

    public MoveResult(boolean success, boolean promoted, boolean continuesCapture, PieceColor nextTurn) {
        this.success = success;
        this.promoted = promoted;
        this.continuesCapture = continuesCapture;
        this.nextTurn = nextTurn;
    }

    public static MoveResult failure() {
        return new MoveResult(false, false, false, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isPromoted() {
        return promoted;
    }

    public boolean isContinuesCapture() {
        return continuesCapture;
    }

    public PieceColor getNextTurn() {
        return nextTurn;
    }
}
