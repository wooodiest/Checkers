package checkers.view;

import checkers.model.Board;
import checkers.model.Move;
import checkers.model.Piece;
import checkers.model.PieceColor;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class BoardPanel extends JPanel {

    private static final Color LIGHT_SQUARE = new Color(240, 217, 181);
    private static final Color DARK_SQUARE = new Color(181, 136, 99);
    private static final Color HIGHLIGHT = new Color(186, 220, 88, 180);
    private static final Color TARGET_HIGHLIGHT = new Color(255, 255, 102, 160);

    private Board board;
    private Point selectedSquare;
    private Set<Point> highlightedTargets = new HashSet<>();
    private Point dragOrigin;
    private Point dragPosition;
    private BiConsumer<Integer, Integer> squareClickListener;

    public BoardPanel() {
        setPreferredSize(new Dimension(640, 640));
        setBackground(Color.DARK_GRAY);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleMousePressed(event);
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                handleMouseDragged(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                handleMouseReleased(event);
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setBoard(Board board) {
        this.board = board;
        clearSelection();
        repaint();
    }

    public void setSquareClickListener(BiConsumer<Integer, Integer> listener) {
        this.squareClickListener = listener;
    }

    public void setSelectedSquare(Integer x, Integer y) {
        if (x == null || y == null) {
            selectedSquare = null;
        } else {
            selectedSquare = new Point(x, y);
        }
        repaint();
    }

    public void setHighlightedTargets(List<Move> moves) {
        highlightedTargets.clear();
        for (Move move : moves) {
            highlightedTargets.add(new Point(move.getToX(), move.getToY()));
        }
        repaint();
    }

    public void clearSelection() {
        selectedSquare = null;
        highlightedTargets.clear();
        dragOrigin = null;
        dragPosition = null;
        repaint();
    }

    private void handleMousePressed(MouseEvent event) {
        if (board == null) {
            return;
        }
        Point square = pixelToSquare(event.getX(), event.getY());
        if (square == null) {
            return;
        }
        Piece piece = board.getPiece(square.x, square.y);
        if (piece != null) {
            dragOrigin = square;
            dragPosition = event.getPoint();
        }
        if (squareClickListener != null) {
            squareClickListener.accept(square.x, square.y);
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (dragOrigin != null) {
            dragPosition = event.getPoint();
            repaint();
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (board == null || dragOrigin == null) {
            dragOrigin = null;
            dragPosition = null;
            return;
        }
        Point target = pixelToSquare(event.getX(), event.getY());
        if (target != null && !target.equals(dragOrigin) && squareClickListener != null) {
            squareClickListener.accept(target.x, target.y);
        }
        dragOrigin = null;
        dragPosition = null;
        repaint();
    }

    private Point pixelToSquare(int pixelX, int pixelY) {
        int squareSize = getSquareSize();
        if (squareSize == 0) {
            return null;
        }
        int x = pixelX / squareSize;
        int y = pixelY / squareSize;
        if (x < 0 || x >= Board.SIZE || y < 0 || y >= Board.SIZE) {
            return null;
        }
        return new Point(x, y);
    }

    private int getSquareSize() {
        return Math.min(getWidth(), getHeight()) / Board.SIZE;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (board == null) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int squareSize = getSquareSize();
        int offsetX = (getWidth() - squareSize * Board.SIZE) / 2;
        int offsetY = (getHeight() - squareSize * Board.SIZE) / 2;

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                int x = offsetX + col * squareSize;
                int y = offsetY + row * squareSize;
                boolean dark = (col + row) % 2 == 1;
                g.setColor(dark ? DARK_SQUARE : LIGHT_SQUARE);
                g.fillRect(x, y, squareSize, squareSize);

                Point square = new Point(col, row);
                if (selectedSquare != null && selectedSquare.equals(square)) {
                    g.setColor(HIGHLIGHT);
                    g.fillRect(x, y, squareSize, squareSize);
                } else if (highlightedTargets.contains(square)) {
                    g.setColor(TARGET_HIGHLIGHT);
                    g.fillRect(x, y, squareSize, squareSize);
                }

                Piece piece = board.getPiece(col, row);
                if (piece != null && !(dragOrigin != null && dragOrigin.equals(square))) {
                    drawPiece(g, piece, x, y, squareSize);
                }
            }
        }

        if (dragOrigin != null && dragPosition != null) {
            Piece draggedPiece = board.getPiece(dragOrigin.x, dragOrigin.y);
            if (draggedPiece != null) {
                int pieceSize = (int) (squareSize * 0.75);
                int drawX = dragPosition.x - pieceSize / 2;
                int drawY = dragPosition.y - pieceSize / 2;
                drawPieceAt(g, draggedPiece, drawX, drawY, pieceSize);
            }
        }

        g.dispose();
    }

    private void drawPiece(Graphics2D g, Piece piece, int squareX, int squareY, int squareSize) {
        int pieceSize = (int) (squareSize * 0.75);
        int drawX = squareX + (squareSize - pieceSize) / 2;
        int drawY = squareY + (squareSize - pieceSize) / 2;
        drawPieceAt(g, piece, drawX, drawY, pieceSize);
    }

    private void drawPieceAt(Graphics2D g, Piece piece, int x, int y, int size) {
        if (piece.getColor() == PieceColor.WHITE) {
            g.setColor(new Color(245, 245, 245));
            g.fillOval(x, y, size, size);
            g.setColor(new Color(60, 60, 60));
        } else {
            g.setColor(new Color(40, 40, 40));
            g.fillOval(x, y, size, size);
            g.setColor(new Color(200, 200, 200));
        }
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawOval(x, y, size, size);

        if (piece.isKing()) {
            g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, size * 0.45f));
            String label = "K";
            int labelWidth = g.getFontMetrics().stringWidth(label);
            int labelHeight = g.getFontMetrics().getAscent();
            g.drawString(label, x + (size - labelWidth) / 2, y + (size + labelHeight) / 2 - 2);
        }
    }
}
