package io.lumen.core.logging;

/**
 * Prints the Lumen startup banner with a perfectly proportioned,
 * 6-line filled yellow light bulb aligned with the framework text.
 */
public final class StartupBanner {

    // ANSI Escape Codes
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_WHITE  = "\u001B[97m";
    private static final String GRAY          = "\u001B[90m";
    private static final String RESET         = "\u001B[0m";
    private static final String BOLD          = "\u001B[1m";

    private static final String VERSION = "0.1.0-SNAPSHOT";

    private static final String BANNER =
            BRIGHT_YELLOW + "    ▄████▄    " + RESET + BRIGHT_WHITE + "██╗     ██╗   ██╗███╗   ███╗███████╗███╗   ██╗\n" +
            BRIGHT_YELLOW + "  ▄████████▄  " + RESET + BRIGHT_WHITE + "██║     ██║   ██║████╗ ████║██╔════╝████╗  ██║\n" +
            BRIGHT_YELLOW + "  ▀████████▀  " + RESET + BRIGHT_WHITE + "██║     ██║   ██║██╔████╔██║█████╗  ██╔██╗ ██║\n" +
            BRIGHT_YELLOW + "    ▀████▀    " + RESET + BRIGHT_WHITE + "██║     ██║   ██║██║╚██╔╝██║██╔══╝  ██║╚██╗██║\n" +
            GRAY          + "     ▄██▄     " + RESET + BRIGHT_WHITE + "███████╗╚██████╔╝██║ ╚═╝ ██║███████╗██║ ╚████║\n" +
            GRAY          + "      ▀▀      " + RESET + BRIGHT_WHITE + "╚══════╝ ╚═════╝ ╚═╝     ╚═╝╚══════╝╚═╝  ╚═══╝\n" +
            "\n" +
            BRIGHT_YELLOW + "  :: Lumen Framework ::        " + BOLD + "(v%s)" + RESET + "\n";

    private StartupBanner() {}

    public static void print() {
        System.out.printf(BANNER, VERSION);
        System.out.flush();
    }
}