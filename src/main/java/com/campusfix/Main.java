package com.campusfix;

import com.campusfix.exception.CampusFixException;
import com.campusfix.model.User;
import com.campusfix.ui.AdminMenu;
import com.campusfix.ui.SubmitterMenu;
import com.campusfix.ui.TechnicianMenu;
import com.campusfix.util.ConsoleUtil;

/**
 * Entry point. Coordinates the start-up menu and hands logged-in users to the menu for their role.
 *
 * <ul>
 *   <li>{@code Main} - starts the application</li>
 *   <li>{@code Main --init-admin} - one-time creation of the first administrator account</li>
 * </ul>
 */
public class Main {

    private final AppContext ctx;

    Main(AppContext ctx) {
        this.ctx = ctx;
    }

    public static void main(String[] args) {
        AppContext ctx = new AppContext();
        Main app = new Main(ctx);
        if (args.length > 0 && args[0].equals("--init-admin")) {
            app.initialiseAdministrator();
        } else {
            app.run();
        }
    }

    private void run() {
        warnIfNoAdministrator();
        while (true) {
            ConsoleUtil.header("CAMPUSFIX\n  Campus Maintenance Management System");
            System.out.println("1. Student Registration");
            System.out.println("2. Faculty Registration");
            System.out.println("3. Login");
            System.out.println("4. Exit");
            switch (ConsoleUtil.readInt("Select an option: ", 1, 4)) {
                case 1 -> ConsoleUtil.runSafely(() -> register(true));
                case 2 -> ConsoleUtil.runSafely(() -> register(false));
                case 3 -> ConsoleUtil.runSafely(this::login);
                default -> {
                    ConsoleUtil.info("Goodbye!");
                    return;
                }
            }
        }
    }

    private void register(boolean student) {
        ConsoleUtil.subHeader(student ? "Student registration" : "Faculty registration");
        String name = ConsoleUtil.readLine("Full name: ");
        String email = ConsoleUtil.readLine("Email: ");
        String phone = ConsoleUtil.readLine("Phone number: ");
        String password = ConsoleUtil.readPassword("Password (min 8 chars, upper, lower, digit, symbol): ");
        String confirm = ConsoleUtil.readPassword("Confirm password: ");
        if (!password.equals(confirm)) {
            ConsoleUtil.error("Passwords do not match.");
            return;
        }
        if (student) {
            ctx.userService.registerStudent(name, email, phone, password);
        } else {
            ctx.userService.registerFaculty(name, email, phone, password);
        }
        ConsoleUtil.success("Registration successful. You can now log in.");
    }

    private void login() {
        ConsoleUtil.subHeader("Login");
        String email = ConsoleUtil.readLine("Email: ");
        String password = ConsoleUtil.readPassword("Password: ");
        User user = ctx.authenticationService.login(email, password);
        ConsoleUtil.success("Welcome, " + user.getName() + "!");
        switch (user.getRole()) {
            case STUDENT, FACULTY -> new SubmitterMenu(ctx).run(user);
            case ADMINISTRATOR -> new AdminMenu(ctx).run(user);
            case TECHNICIAN -> new TechnicianMenu(ctx).run(user);
        }
    }

    /** Secure first-time setup: the password is typed by a human, hashed, and never written anywhere else. */
    private void initialiseAdministrator() {
        ConsoleUtil.header("CAMPUSFIX - ONE-TIME ADMINISTRATOR SETUP");
        try {
            if (ctx.userService.administratorExists()) {
                ConsoleUtil.error("An administrator already exists. Setup is disabled.");
                return;
            }
            String name = ConsoleUtil.readLine("Administrator name: ");
            String email = ConsoleUtil.readLine("Administrator email: ");
            String phone = ConsoleUtil.readLine("Phone number: ");
            String password = ConsoleUtil.readPassword("Password (min 8 chars, upper, lower, digit, symbol): ");
            if (!password.equals(ConsoleUtil.readPassword("Confirm password: "))) {
                ConsoleUtil.error("Passwords do not match. Nothing was created.");
                return;
            }
            ctx.userService.createInitialAdministrator(name, email, phone, password);
            ConsoleUtil.success("Administrator account created. Start the application normally and log in.");
        } catch (CampusFixException e) {
            ConsoleUtil.error(e.getMessage());
        }
    }

    private void warnIfNoAdministrator() {
        try {
            if (!ctx.userService.administratorExists()) {
                ConsoleUtil.info("NOTE: no administrator account exists yet. Stop the app and run it with --init-admin (see README).");
            }
        } catch (CampusFixException e) {
            ConsoleUtil.error(e.getMessage());
            ConsoleUtil.info("You can still use the menu, but database features will not work until this is fixed.");
        }
    }
}
