package server;

public class Board {
    private static final int SIZE = 9;
    private final char[] cells = new char[SIZE];

    public Board() {
        for (int i = 0; i < SIZE; i++) {
            cells[i] = '_';
        }
    }

    public boolean makeMove(int position, char symbol) {
        if (position < 0 || position >= SIZE || cells[position] != '_') {
            return false;
        }

        cells[position] = symbol;
        return true;
    }

    public boolean hasWinner(char symbol) {
        int[][] lines = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
                {0, 4, 8}, {2, 4, 6}
        };

        for (int[] line : lines) {
            if (cells[line[0]] == symbol && cells[line[1]] == symbol && cells[line[2]] == symbol) {
                return true;
            }
        }

        return false;
    }

    public boolean isFull() {
        for (char cell : cells) {
            if (cell == '_') {
                return false;
            }
        }

        return true;
    }

    public String serialize() {
        return new String(cells);
    }
}
