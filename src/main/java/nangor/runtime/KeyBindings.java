package nangor.runtime;

import nangor.core.BombType;
import nangor.core.Direction;
import nangor.core.GameCommand;
import nangor.core.GameState;

public final class KeyBindings {
    public static final int UP = 1000, RIGHT = 1001, DOWN = 1002, LEFT = 1003;
    private KeyBindings() { }

    public static GameCommand decode(int key, GameState state) {
        if (key == -1 || key == 3) return GameCommand.Action.EXIT;
        int c = key >= UP && key <= LEFT ? key : Character.toLowerCase(key);
        if (state == GameState.MENU) return switch (c) {
            case '1', '\r', '\n' -> GameCommand.Action.START;
            case '2', 'h' -> GameCommand.Action.HELP;
            case '3', 'q' -> GameCommand.Action.EXIT;
            default -> GameCommand.Action.INVALID;
        };
        if (state == GameState.HELP) return GameCommand.Action.BACK;
        if (state == GameState.PAUSED || state == GameState.WIN || state == GameState.LOSE) return switch (c) {
            case 'p', 'c' -> GameCommand.Action.PAUSE;
            case 'r' -> GameCommand.Action.RESTART;
            case 'm' -> GameCommand.Action.BACK;
            case 'q' -> GameCommand.Action.EXIT;
            default -> GameCommand.Action.INVALID;
        };
        return switch (c) {
            case 'w', UP -> new GameCommand.Move(Direction.UP);
            case 'd', RIGHT -> new GameCommand.Move(Direction.RIGHT);
            case 's', DOWN -> new GameCommand.Move(Direction.DOWN);
            case 'a', LEFT -> new GameCommand.Move(Direction.LEFT);
            case '1' -> new GameCommand.Select(BombType.NORMAL);
            case '2', 'e' -> new GameCommand.Select(BombType.PUMPED);
            case '3' -> new GameCommand.Select(BombType.REMOTE);
            case 'b', ' ' -> GameCommand.Action.PLACE;
            case 'q' -> GameCommand.Action.DETONATE;
            case 'p' -> GameCommand.Action.PAUSE;
            default -> GameCommand.Action.INVALID;
        };
    }
}
