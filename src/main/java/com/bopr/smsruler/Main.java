package com.bopr.smsruler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
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

    private final Api api;

    private Main(String port) {
        api = new Api(port);
        api.addListener(new ApiListener() {

            @Override
            void onInfo(List<String> info) {
                info.forEach(System.out::println);
            }

            @Override
            void onMessageReceived(int messageId) {
                api.readMessage(messageId);
            }

            @Override
            void onMessagesRead(List<Message> messages) {
                handelMessages(messages);
            }

            @Override
            void onRings(String phone, int count) {
                Message message = new Message();
                message.setPhone(phone);
                message.setText("[start " + count + "-rings.bat]");
                handelMessages(Collections.singletonList(message));
            }
        });
    }

    private void run() throws Exception {
        api.open();
        System.out.println("Listening port: " + api.getPort());
        api.info();
        readConsole();
        api.close();
    }

    private void handelMessages(List<Message> messages) {
        for (Message message : messages) {
            if (!message.isRead()) {
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
        String phone = message.getPhone();
        if (phone != null && !phone.isEmpty()) {
            log.info("Reply: " + status);
            api.sendMessage(phone, status);
        } else {
            log.info("Cannot reply. No phone number");
        }
    }

    private void readConsole() {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            switch (scanner.next().toUpperCase()) {
                case "EXIT":
                    return;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main(args[0]).run();
    }

}
