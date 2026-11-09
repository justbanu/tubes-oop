package nangor.core;

/** Commands carry intent only; only the game thread executes them. */
public interface GameCommand {
    void execute(GameEngine engine);

    record Move(Direction direction) implements GameCommand {
        @Override public void execute(GameEngine engine) { engine.move(direction); }
    }
    record Select(BombType type) implements GameCommand {
        @Override public void execute(GameEngine engine) { engine.select(type); }
    }
    enum Action implements GameCommand {
        START, HELP, BACK, EXIT, PLACE, DETONATE, PAUSE, RESTART, INVALID;
        @Override public void execute(GameEngine engine) { engine.action(this); }
    }
}
