package com.bopr.smsruler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Class Main.
 *
 * @author Boris Pronin (<a href="mailto:boprsoft.dev@gmail.com">boprsoft.dev@gmail.com</a>)
 */
public class Main {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String PORT = "COM3";
    private final Api api;

    private Main() {
        api = new Api(PORT);
        api.addListener(new ApiListener() {

            @Override
            void onInfo(String info) {
                System.out.println(info);
            }

            @Override
            void onMessageReceived(int messageId) {
                api.listMessages();
            }

            @Override
            void onListMessages(List<Message> messages) {
                handelMessages(messages);
            }
        });
    }

    private void run() throws Exception {
//        api.open();
//        log.info("Running");

//        api.info();
//        api.listMessages();
//        api.sendMessage("+79052309441", "This is a test message from sms ruler");
//        Thread.sleep(500);

//        readConsole();
//        api.close();
        Message message = new Message();
        message.setText("echo");
        execute(message, System.out::println);
    }

    private void handelMessages(List<Message> messages) {
        for (Message message : messages) {
            if (message.isRead()) {
                execute(message, status -> reply(message, status));
            }
        }
    }

    private void execute(Message message, Consumer<String> callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new ProcessTask(message, callback));
        executor.shutdown();
    }

    private void reply(Message message, String status) {
        api.sendMessage(message.getPhone(), status + ": " + message.getText());
    }

    private void readConsole() {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String command = scanner.next().toUpperCase();
            switch (command) {
                case "EXIT":
                case "QQQ":
                    return;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run();
    }

}
