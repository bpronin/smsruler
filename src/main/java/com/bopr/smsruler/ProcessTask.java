package com.bopr.smsruler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Class ProcessTask.
 *
 * @author Boris Pronin (<a href="mailto:boprsoft.dev@gmail.com">boprsoft.dev@gmail.com</a>)
 */
public class ProcessTask implements Runnable {

    private static final long TIMEOUT = 10000;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Message message;
    private Consumer<String> callback;

    ProcessTask(Message message, Consumer<String> callback) {
        this.message = message;
        this.callback = callback;
    }

    @Override
    public void run() {
        String command = message.getText().toLowerCase();
        log.debug("Executing: " + command);
        try {
            long start = System.currentTimeMillis();
            long elapsed = 0;

            Process process = Runtime.getRuntime().exec(formatCommand(command));
            while (process.isAlive() && (elapsed <= TIMEOUT)) {
                elapsed = System.currentTimeMillis() - start;
            }

            if (elapsed <= TIMEOUT) {
                callback.accept("Executed (" + process.exitValue() + ")");
            } else {
                process.destroy();
                log.error("Timeout expired");
                callback.accept("Timeout expired");
            }
        } catch (IOException x) {
            log.error("Cannot execute command", x);
            callback.accept(x.getMessage());
        }
    }

    private String formatCommand(String command) {
        return "cmd /c start " + command + ".bat";
    }

}
