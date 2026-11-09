package nangor.core;

enum Block {
    INDESTRUCTIBLE('#'), DESTRUCTIBLE('D'), REINFORCED('R');
    final char symbol;
    Block(char symbol) { this.symbol = symbol; }
    boolean destroyedBy(BombType type) {
        return this == DESTRUCTIBLE || this == REINFORCED && type == BombType.PUMPED;
    }
}
