package exec;

import java.io.BufferedWriter;
import java.io.IOException;

public class Util {
    static void bufferedWriterWrite(BufferedWriter bw, String str) {
        try {
            bw.write(str);
        } catch (IOException e) {
            System.err.println("Failed to write the str " + str);
            throw new RuntimeException(e);
        }
    }
}
