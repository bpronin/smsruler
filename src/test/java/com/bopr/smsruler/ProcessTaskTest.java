package com.bopr.smsruler;

import org.junit.Test;

public class ProcessTaskTest {

    @Test
    public void run() throws Exception {
        Message m = new Message();
        m.setText("93.92.18.2909:98373 [start echo.bat]");
//        m.setText("[echo YAH >> test.txt]");
        new ProcessTask(m, System.out::println).run();
    }

}