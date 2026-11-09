package nangor.core;

public enum AttackPattern {
    FIRE_BREATH, BITE, SWOOP, WING_GUST;
    long duration(int phase) {
        return switch (this) {
            case BITE -> 750;
            case FIRE_BREATH, SWOOP -> phase == 2 ? 500 : 1000;
            case WING_GUST -> 1000;
        };
    }
    boolean opensVulnerability() { return this == FIRE_BREATH || this == SWOOP; }
}
