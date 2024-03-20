package org.example;

import java.io.IOException;
import java.util.logging.*;

import static java.util.logging.Level.INFO;

public class Logging {

    public static void setupLogger() throws IOException {
        // Create logger
        Logger logger = Logger.getLogger("simple_logger");

        // Set logging level
        logger.setLevel(Level.FINER);

        // Create rotating file handler and set level to debug
        FileHandler handler = new FileHandler("cnpr_app.log",0, 2, true);
        handler.setLevel(Level.FINER);

        // Create formatters
        SimpleFormatter simpleFormatter = new SimpleFormatter();
        Formatter detailedFormatter = new Formatter() {
            @Override
            public String format(LogRecord record) {
                return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %2$s[%3$d]: %4$s - %5$s%n",
                        record.getMillis(),
                        record.getLoggerName(),
                        record.getThreadID(),
                        record.getLevel(),
                        record.getMessage());
            }
        };

        // Get a top-level "mypackage" logger, set its log level to DEBUG,
        // but prevent it from propagating messages to the root logger
        logger = Logger.getLogger("cnrp_app");
        logger.setLevel(Level.FINER);
        logger.setUseParentHandlers(false);

        // Create a console handler and set its log level
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(INFO);
        consoleHandler.setFormatter(simpleFormatter);

        // Create a file handler and set its log level
        FileHandler fileHandler = new FileHandler("cnpr_app.log", 1024 * 1024, 5, true);
        fileHandler.setLevel(Level.FINER);
        fileHandler.setFormatter(detailedFormatter);
        //handler.setFormatter(detailedFormatter);

        // Add handlers to logger
        logger.addHandler(consoleHandler);
        logger.addHandler(handler);
    }


}
