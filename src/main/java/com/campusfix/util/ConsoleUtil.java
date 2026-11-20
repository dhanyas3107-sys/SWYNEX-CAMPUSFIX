package com.campusfix.util;

import com.campusfix.exception.CampusFixException;

import java.io.Console;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** All console input/output helpers, so menus stay short and input handling is consistent. */
public final class ConsoleUtil {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final String LINE = "=".repeat(60);

    private ConsoleUtil() { }

    public static void header(String title) {
        System.out.println();
        System.out.println(LINE);
        System.out.println("  " + title);
        System.out.println(LINE);
    }

    public static void subHeader(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    public static void success(String message) {
        System.out.println("[OK]    " + message);
    }

    public static void error(String message) {
        System.out.println("[ERROR] " + message);
    }

    public static void info(String message) {
        System.out.println(message);
    }

    /** Reads one trimmed line. If the input stream is closed (Ctrl+D) the program exits cleanly. */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return SCANNER.nextLine().trim();
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Input closed. Goodbye!");
            System.exit(0);
            return ""; // unreachable
        }
    }

    /** Reads a password without echo when a real terminal is available. */
    public static String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            return chars == null ? "" : new String(chars);
        }
        System.out.println("(Note: this terminal cannot hide typing.)");
        return readLine(prompt);
    }

    /** Keeps asking until the user types a whole number between min and max (inclusive). */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            error("Please enter a number between " + min + " and " + max + ".");
        }
    }

    /** Shows a numbered list of the values and returns the one the user picks. */
    public static <T> T choose(String title, List<T> options, java.util.function.Function<T, String> label) {
        System.out.println(title);
        for (int i = 0; i < options.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, label.apply(options.get(i)));
        }
        return options.get(readInt("Choose (1-" + options.size() + "): ", 1, options.size()) - 1);
    }

    public static <E extends Enum<E>> E chooseEnum(String title, E[] values) {
        return choose(title, List.of(values), Enum::name);
    }

    public static boolean confirm(String prompt) {
        while (true) {
            String answer = readLine(prompt + " (y/n): ").toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            error("Please answer y or n.");
        }
    }

    public static void pause() {
        readLine("\nPress Enter to continue...");
    }

    /**
     * Runs one menu action. Expected problems (validation, permissions, database) become a friendly
     * message instead of crashing the program. Set CAMPUSFIX_DEBUG=true to also print stack traces.
     */
    public static void runSafely(Runnable action) {
        try {
            action.run();
        } catch (CampusFixException e) {
            error(e.getMessage());
            if (isDebug() && e.getCause() != null) {
                e.getCause().printStackTrace();
            }
        } catch (RuntimeException e) {
            error("Something unexpected went wrong. Please try again.");
            if (isDebug()) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isDebug() {
        return "true".equalsIgnoreCase(System.getenv("CAMPUSFIX_DEBUG"));
    }
}
