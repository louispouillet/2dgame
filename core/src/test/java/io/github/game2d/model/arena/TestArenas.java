package io.github.game2d.model.arena;

public final class TestArenas {
    private TestArenas() {
    }

    public static Arena open(int width, int height) {
        StringBuilder map = new StringBuilder();
        map.append("#".repeat(width)).append('\n');
        map.append('#').append('1').append(".".repeat(width - 4)).append('2').append('#').append('\n');
        for (int line = 2; line < height - 1; line++) {
            map.append('#').append(".".repeat(width - 2)).append('#').append('\n');
        }
        map.append("#".repeat(width)).append('\n');
        return ArenaLoader.parse(map.toString());
    }
}
