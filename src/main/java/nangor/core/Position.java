package nangor.core;

public record Position(int x, int y) {
    public Position step(Direction direction) {
        return new Position(x + direction.dx, y + direction.dy);
    }

    public int distance(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }
}
