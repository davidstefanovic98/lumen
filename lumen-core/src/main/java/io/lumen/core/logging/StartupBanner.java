package io.lumen.core.logging;

/**
 * Prints Lumen startup banner and version info.
 */
public final class StartupBanner {

    private static final String BANNER = """

  ██╗     ██╗   ██╗███╗   ███╗███████╗███╗   ██╗
  ██║     ██║   ██║████╗ ████║██╔════╝████╗  ██║
  ██║     ██║   ██║██╔████╔██║█████╗  ██╔██╗ ██║
  ██║     ██║   ██║██║╚██╔╝██║██╔══╝  ██║╚██╗██║
  ███████╗╚██████╔╝██║ ╚═╝ ██║███████╗██║ ╚████║
  ╚══════╝ ╚═════╝ ╚═╝     ╚═╝╚══════╝╚═╝  ╚═══╝

  :: Lumen Framework ::        (v%s)
        
""";

    private static final String VERSION = "0.1.0-SNAPSHOT";

    private StartupBanner() {}

    public static void print(Logger logger) {
        String banner = BANNER.formatted(VERSION);
        for (String line : banner.split("\n")) {
            logger.info(line);
        }
    }
}