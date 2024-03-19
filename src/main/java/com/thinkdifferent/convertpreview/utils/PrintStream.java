package com.thinkdifferent.convertpreview.utils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class PrintStream extends Thread {
    java.io.InputStream __is = null;

    public PrintStream(java.io.InputStream is) {
        __is = is;
    }

    public void run() {
        try {
            while (this != null) {
                int _ch = __is.read();
                if (_ch != -1)
                    System.out.print((char) _ch);
                else break;
            }
        } catch (Exception | Error e) {
            log.error("PrintStream", e);
        }
    }
}
