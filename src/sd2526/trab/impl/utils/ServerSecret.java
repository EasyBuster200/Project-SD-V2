package sd2526.trab.impl.utils;

/**
 * Shared secret used to authenticate admin endpoints between servers.
 * 
 * Secret is read once from the command-line arguments and then exposed via the
 * static {@link #get()}
 */
public class ServerSecret {

    public static final String HEADER = "X-Secret";
    public static final String ARG = "-secret";

    private static String secret = "";

    public static void set(String value) {
        secret = value == null ? "" : value;
    }

    public static String get() {
        return secret;
    }

    /**
     * Scans the command line arguments for {@code -secret <value>} and stashes the
     * value.
     */
    public static void parse(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if (ARG.equals(args[i])) {
                set(args[i + 1]);
                return;
            }
        }
    }
}