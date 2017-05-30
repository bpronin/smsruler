package com.bopr.smsruler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String text = message.getText().toLowerCase();
        log.debug("Executing: " + text);
        try {
            long start = System.currentTimeMillis();
            long elapsed = 0;

            String command = formatCommand(text);
            log.debug("Executing: " + command);
            Process process = Runtime.getRuntime().exec(command);
            while (process.isAlive() && (elapsed <= TIMEOUT)) {
                elapsed = System.currentTimeMillis() - start;
            }

            if (elapsed <= TIMEOUT) {
                callback.accept("Command executed with code " + process.exitValue());
            } else {
                process.destroy();
                log.error("Timeout expired");
                callback.accept("Cannot execute command. Timeout expired");
            }
        } catch (Exception x) {
            log.error("Cannot execute command", x);
            callback.accept("Exception raised when executing");
        }
    }

    private String formatCommand(String text) throws Exception {
        Matcher matcher = Pattern.compile("\\((.*?)\\)").matcher(text);
        if (matcher.find()) {
            return "cmd /c " + matcher.group(1);
        } else {
            throw new Exception("Invalid command");
        }
    }

}
