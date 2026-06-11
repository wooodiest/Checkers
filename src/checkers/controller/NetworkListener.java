package checkers.controller;

import checkers.model.PieceColor;

public interface NetworkListener {

    void onConnected(PieceColor assignedColor);

    void onChatMessage(String sender, String message);

    void onRemoteMove(int fromX, int fromY, int toX, int toY);

    void onGameEnd(String reason);

    void onDisconnected();
}
