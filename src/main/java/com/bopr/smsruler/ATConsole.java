package com.bopr.smsruler;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Class ATConsole.
 *
 * @author Boris Pronin (<a href="mailto:boprsoft.dev@gmail.com">boprsoft.dev@gmail.com</a>)
 */
public class ATConsole {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private SerialPort port;

    public static void main(String[] args) throws Exception {
        new ATConsole().run("COM3");
    }

    private void run(String portName) throws SerialPortException {
        start(portName);

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String command = scanner.next();
            if (command.equalsIgnoreCase("qqq")) {
                break;
            } else {
                write(command);
            }
        }

        stop();
    }

    private void start(String portName) throws SerialPortException {
        port = new SerialPort(portName);
        port.openPort();
        port.addEventListener(e -> {
            System.err.println("EVENT type:" + e.getEventType());
            try {
                System.out.println(read());
            } catch (SerialPortException x) {
                log.error("Invalid response", x);
            }
        });
    }

    private void stop() throws SerialPortException {
        port.closePort();
    }

    private String read() throws SerialPortException {
        return port.readString();
    }

    private void write(String command) throws SerialPortException {
        port.writeString(command + "\r");
    }

}
