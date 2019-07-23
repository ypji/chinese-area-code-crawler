package exec;

import com.google.common.base.Splitter;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 将Executor生成的CSV文件转成我想要的SQL
 */
public class SQLGen {
    private static final String TARGET_FILE = "D:\\34.sql";
    private static final String SOURCE_FILE = "D:\\34.csv";

    public static void main(String[] args) throws IOException {
        List<List<Area>> lists = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            lists.add(new ArrayList<>());
        }

        final Splitter SPLITTER = Splitter.on(',');
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(TARGET_FILE), StandardCharsets.UTF_8))) {

            for (String line : FileUtils.readLines(new File(SOURCE_FILE), StandardCharsets.UTF_8)) {
                if (line.isEmpty()) {
                    return;
                }
                Iterator<String> split = SPLITTER.split(line).iterator();
                Area area = new Area();
                area.code = split.next();
                area.name = split.next();
                area.fullName = split.next();
                area.parent = split.next();

                switch (area.code.length()) {
                    case 2:
                        lists.get(0).add(area);
                        break;
                    case 4:
                        lists.get(1).add(area);
                        break;
                    case 6:
                        lists.get(2).add(area);
                        break;
                    case 9:
                        lists.get(3).add(area);
                        break;
                    case 12:
                        lists.get(4).add(area);
                        break;
                }
            }

            lists.forEach(list -> {
                list.forEach(area -> writeSQL(area, bw));
                flush(bw); // 为了避免出现外键插入失败问题，不同的级别放到不同的SQL中
            });
        }
    }

    private static ArrayList<Area> writeSQLBuffer = new ArrayList<>(300);
    private static void writeSQL(Area area, BufferedWriter bw) {
        writeSQLBuffer.add(area);
        if (writeSQLBuffer.size() == 300) {
            flush(bw);
        }
    }
    private static void flush(BufferedWriter bw) {
        int bufferSize = writeSQLBuffer.size();
        if (bufferSize > 0) {
            Util.bufferedWriterWrite(bw, "insert into t_area(code, name, fullName, parent) values\n");
            for (int i = 0; i < bufferSize; i++) {
                Area a = writeSQLBuffer.get(i);
                String line = String.format("    ('%s','%s','%s', %s)%s\n",
                        a.code,
                        a.name,
                        a.fullName,
                        ("null".equals(a.parent) ? "null" : "'" + a.parent + "'"),
                        (i == bufferSize - 1) ? ";" : ",");
                Util.bufferedWriterWrite(bw, line);
            }
            writeSQLBuffer = new ArrayList<>(300);
        }
    }

    private static final class Area {
        private String code;
        private String name;
        private String fullName;
        private String parent;
    }
}
