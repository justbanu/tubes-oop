package nangor.core;

abstract class Entity {
    final long id;
    Position position;
    Entity(long id, Position position) { this.id = id; this.position = position; }
}
