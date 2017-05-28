package com.bopr.smsruler;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class Api.
 *
 * @author Boris Pronin (<a href="mailto:boprsoft.dev@gmail.com">boprsoft.dev@gmail.com</a>)
 */
@SuppressWarnings("WeakerAccess")
public class Api {

    public static final char CTRL_Z = 26;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private SerialPort port;
    private String portName;
    private ApiListener listener;
    private String predResponse;

    public Api(String portName) {
        this.portName = portName;
    }

    public String getPort() {
        return port.getPortName();
    }

    public void open() throws SerialPortException {
        port = new SerialPort(portName);
        port.openPort();
        port.addEventListener(new SerialPortListener());
        log.trace("Port " + portName + " open");
    }

    public void close() throws SerialPortException {
        port.closePort();
        log.trace("Port " + portName + " closed");
    }

    public void info() {
        try {
            write("ATI");
        } catch (SerialPortException x) {
            log.error("Cannot obtain info", x);
        }
    }

    public void listMessages() {
        try {
            write("AT+CMGF=1");
            write("AT+CMGL=\"ALL\"");
        } catch (SerialPortException x) {
            log.error("Cannot list messages", x);
        }
    }

    public void sendMessage(String phone, String text) {
        try {
            write("AT+CMGF=1");
            write("AT+CMGS=\"" + phone + "\"");
            write(text + CTRL_Z);
        } catch (SerialPortException x) {
            log.error("Cannot send messages", x);
        }
    }

    public void deleteMessage(int id) {
        try {
            write("AT+CMGF=1");
            write("AT+CMGD=" + id);
        } catch (SerialPortException x) {
            log.error("Cannot delete message", x);
        }
    }

    private void dispatch(String response) {
        if (response.startsWith("+CMTI")) {
            int messageId = Integer.valueOf(response.split(",")[1]); /* "+CMTI: \"ME\",0" */
            listener.onMessageReceived(messageId);
        } else if (response.startsWith("+CMGL")) {
            listener.onListMessages(parseMessages(response));
        } else if (response.startsWith("+CMGS")) {
            log.debug("Message sent");
        } else if (response.startsWith(">")) {
            log.debug("Prompt");
        } else if (response.startsWith("OK")) {
            log.debug("OK");
        } else if (response.startsWith("ERROR")) {
            log.debug("ERROR");
        } else {
            log.warn("Unhandled: " + response);
        }
    }

    private List<Message> parseMessages(String response) {
        List<Message> list = new ArrayList<>();
        try {
            String[] lines = response.split("\r\n");

            for (int i = 0; i < lines.length && !lines[i].isEmpty(); i++) {
                String[] fields = lines[i].split(",");

                Message message = new Message();
                message.setId(Integer.valueOf(fields[0].split(":")[1].trim()));
                message.setRead(!fields[1].contains("UNREAD"));
                message.setPhone(unquote(fields[2].trim()));
                message.setDate(unquote(fields[4].trim()));
                message.setTime(unquote(fields[5].trim()));
                message.setText(lines[++i].trim());

                list.add(message);
            }
        } catch (Throwable x) {
            log.error("Message parsing error", x);
        }
        return list;
    }

    private String unquote(String s) {
        String result = s;
        if (result.startsWith("\"")) {
            result = result.substring(1);
        }
        if (result.endsWith("\"")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String read(int count) throws SerialPortException {
        return port.readString(count)
                //      .trim()
                //      .replaceAll("\r\n","<cr>")
                ;
    }

//     private String read() throws SerialPortException {
//        StringBuilder sb = new StringBuilder();
//        String line = port.readString();
//        while (line != null) {
//            sb.append(line);
//            line = port.readString();
//        }
//        return sb.toString();
//    }

    private void write(String command) throws SerialPortException {
        port.writeString(command + "\r");
        log.debug("Sent:\n---\n" + command + "\n---");
    }

    public void addListener(ApiListener listener) {
        this.listener = listener;
    }

    private class SerialPortListener implements SerialPortEventListener {

        private StringBuilder builder = new StringBuilder();

        @Override
        public void serialEvent(SerialPortEvent e) {
            log.debug("Port event type: " + e.getEventType() + " value: " + e.getEventValue());
            if (e.getEventType() == SerialPortEvent.RXCHAR) {
                try {
                    String chunk = read(e.getEventValue());
                    builder.append(chunk);
//                    System.err.println(chunk);
                    if (chunk.endsWith("\r\nOK\r\n")) {
                        String response = builder.toString().trim();
                        builder = new StringBuilder();
                        log.debug("Received:\n---\n" + response + "\n---");
                        dispatch(response);
                    }
                } catch (SerialPortException x) {
                    log.error("Invalid response", x);
                }
            }
        }

    }
}
