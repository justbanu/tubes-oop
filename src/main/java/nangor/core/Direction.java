package nangor.core;

public enum Direction {
    UP(0, -1), RIGHT(1, 0), DOWN(0, 1), LEFT(-1, 0);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }

    static Direction toward(Position from, Position to) {
        int dx = to.x() - from.x(), dy = to.y() - from.y();
        if (Math.abs(dx) >= Math.abs(dy) && dx != 0) return dx > 0 ? RIGHT : LEFT;
        return dy > 0 ? DOWN : UP;
    }
}
