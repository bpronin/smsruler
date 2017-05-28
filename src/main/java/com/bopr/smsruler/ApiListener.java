package com.bopr.smsruler;

import java.util.List;

/**
 * Class ApiListener.
 *
 * @author Boris Pronin (<a href="mailto:boprsoft.dev@gmail.com">boprsoft.dev@gmail.com</a>)
 */
public abstract class ApiListener {

    void onInfo(String info) {
    }

    void onMessageReceived(int messageId) {
    }

    void onListMessages(List<Message> messages) {
    }
}
