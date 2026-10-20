package utils;

import model.User;

/** Holds the currently logged-in user. */
public final class Session {
    private static User current;

    private Session() {}

    public static void start(User u) { current = u; }
    public static void end() { current = null; }
    public static User getUser() { return current; }
    public static int userId() { return current == null ? 0 : current.getId(); }
    public static String fullName() { return current == null ? "" : current.getFullName(); }
}
