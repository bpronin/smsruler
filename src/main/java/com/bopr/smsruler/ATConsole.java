package com.bopr.smsruler;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.util.Scanner;

/**
 * Class ATConsole.
 *
 * @author Boris Pronin (<a href="mailto:boprsoft.dev@gmail.com">boprsoft.dev@gmail.com</a>)
 */
public class ATConsole {

    public static void main(String[] args) throws Exception {
        SerialPort port = new SerialPort("COM3");
        port.openPort();
        port.addEventListener(e -> {
            System.out.print("<EVENT>");
            try {
                String text = port.readString();
                if (text != null) {
                    System.out.print(text
                                    .replaceAll("\r\n", "<CR>\n").replaceAll("\r", "<R>\n")
//                            .replaceAll("\r\n", "<CR>\n")
                    );
                } else {
                    System.out.print("<NULL>");
                }
            } catch (SerialPortException x) {
                throw new RuntimeException("Invalid response", x);
            }
        });

        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String command = scanner.next();
            if (command.equalsIgnoreCase("qqq")) {
                break;
            } else {
                port.writeString(command + "\r\n");
            }
        }

        port.closePort();
    }

}
