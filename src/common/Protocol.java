package common;

public final class Protocol {
    public static final String MOVE = "MOVE";
    public static final String BOARD = "BOARD";
    public static final String STATUS = "STATUS";
    public static final String MESSAGE = "MESSAGE";
    public static final String ASSIGN = "ASSIGN";
    public static final String ERROR = "ERROR";
    public static final String QUIT = "QUIT";

    public static final String YOUR_TURN = "YOUR_TURN";
    public static final String WAIT = "WAIT";
    public static final String WIN = "WIN";
    public static final String DRAW = "DRAW";
    public static final String LOSE = "LOSE";
    public static final String OPPONENT_LEFT = "OPPONENT_LEFT";

    public static final String SEPARATOR = ":";

    private Protocol() {
    }

    public static String command(String type, String value) {
        return type + SEPARATOR + value;
    }
}
