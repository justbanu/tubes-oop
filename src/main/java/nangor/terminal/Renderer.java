package nangor.terminal;

import nangor.core.GameSnapshot;

@FunctionalInterface
public interface Renderer {
    void render(GameSnapshot snapshot);
}
