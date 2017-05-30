package com.bopr.smsruler;


import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class Api.
 *
 * @author Boris Pronin (<a href="mailto:boprsoft.dev@gmail.com">boprsoft.dev@gmail.com</a>)
 */
@SuppressWarnings({"WeakerAccess", "Duplicates"})
public class Api {

    public static final char CTRL_Z = 26;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private jssc.SerialPort port;
    private String portName;
    private ApiListener listener;
    private ExecutorService executor;

    public Api(String portName) {
        this.portName = portName;
    }

    public String getPort() {
        return port.getPortName();
    }

    public void open() throws SerialPortException {
        port = new jssc.SerialPort(portName);
        port.openPort();
        executor = Executors.newSingleThreadExecutor();
        port.addEventListener(new SerialPortListener(), SerialPortEvent.RXCHAR);
        log.trace("Port " + portName + " open");
    }

    public void close() throws SerialPortException {
        port.closePort();
        executor.shutdown();
        log.trace("Port " + portName + " closed");
    }

    public void info() {
        send("ATI");
    }

    public void listMessages() {
        send("AT+CMGF=1");
        send("AT+CMGL=\"ALL\"");
    }

    public void sendMessage(String phone, String text) {
        send("AT+CMGF=1");
        send("AT+CMGS=\"" + phone + "\"");
        send(text + CTRL_Z);
    }

    public void deleteMessage(int id) {
        send("AT+CMGF=1");
        send("AT+CMGD=" + id);
    }

    private void send(String s) {
        try {
            port.writeString(s + "\r\n");
            log.debug("Sent:\n---\n" + s + "\n---");
        } catch (SerialPortException x) {
            log.error("Cannot send : " + s, x);
        }
    }

    private void dispatch(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("ATI")) {
                i = handleATI(lines, i);
            } else if (lines[i].startsWith("AT+CMGL")) {
                i = handleCMGL(lines, i);
            } else if (lines[i].startsWith("+CMGS")) {
                log.debug("Message sent");
            } else if (lines[i].endsWith("\r")) {
                log.debug("COMMAND: " + lines[i]);
            } else if (lines[i].equals("OK")) {
                log.debug("OK");
            } else if (lines[i].equals("ERROR")) {
                log.debug("ERROR");
            } else {
                log.debug("Unhandled: " + lines[i]);
            }
        }
    }

    private int handleATI(String[] lines, int start) {
        List<String> list = new ArrayList<>();
        int i = start;
        i++;
        while (i < lines.length && !lines[i].equals("OK")) {
            if (!lines[i].isEmpty()) {
                list.add(lines[i]);
            }
            i++;
        }
        listener.onInfo(list);
        return i;
    }

    private int handleCMGL(String[] lines, int start) {
        List<Message> list = new ArrayList<>();
        int i = start;
        i++;
        while (i < lines.length && !lines[i].equals("OK")) {
            if (!lines[i].isEmpty()) {
                String[] fields = lines[i].split(",");

                Message message = new Message();
                message.setId(Integer.valueOf(fields[0].split(":")[1].trim()));
                message.setRead(!fields[1].contains("UNREAD"));
                message.setPhone(unquote(fields[2].trim()));
                message.setDate(unquote(fields[4].trim()));
                message.setTime(unquote(fields[5].trim()));

                i++;
                message.setText(lines[i].trim());

                list.add(message);
            }
            i++;
        }
        listener.onListMessages(list);
        return i;
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

    public void addListener(ApiListener listener) {
        this.listener = listener;
    }

    private class SerialPortListener implements SerialPortEventListener {

        private StringBuilder buffer;

        @Override
        public void serialEvent(SerialPortEvent e) {
            log.trace("Port event type: " + e.getEventType() + " value: " + e.getEventValue());
            try {
                String chunk = port.readString();
                if (chunk != null && !chunk.isEmpty()) {
                    if (buffer == null) {
                        buffer = new StringBuilder();
                    }
                    buffer.append(chunk);

                    if (chunk.endsWith("\r\n")) {
                        String lines = buffer.toString();
                        buffer = null;
                        log.trace("Received:\n---\n" + lines + "\n---");
                        dispatch(lines.split("\r\n"));
                    }
                }
            } catch (SerialPortException x) {
                log.error("Invalid response", x);
            }
        }
    }

}
