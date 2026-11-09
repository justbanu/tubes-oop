package nangor.core;

public enum PowerUpType {
    BOMB_UP('+'), FIRE_UP('f'), SPEED_UP('s'), REMOTE('r'), HEART('h');
    public final char symbol;
    PowerUpType(char symbol) { this.symbol = symbol; }
}
