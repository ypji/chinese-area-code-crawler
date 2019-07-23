package exec;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 从国家统计局网站获取数据并写入到csv
 * 一次执行只能爬取一个省的数据
 */
public class Executor {
    /**
     * 爬取安徽省， 修改可以爬取其他省的
     */
    private static final Area ROOT_AREA = new Area("34", "安徽省", "安徽省", null, false);

    private static final String TARGET_FILE = "D:\\34.csv";

    private static OkHttpClient HTTP_CLIENT = new OkHttpClient();

    public static void main(String[] args) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(TARGET_FILE), StandardCharsets.UTF_8))) {
            Util.bufferedWriterWrite(bufferedWriter,
                    ROOT_AREA.code + ',' + ROOT_AREA.name + ',' + ROOT_AREA.fullName + ',' + ROOT_AREA.parent + '\n');
            resolveAll(ROOT_AREA, bufferedWriter);
        }
    }

    private static int resolvedCount = 0;

    private static void resolveAll(Area parentArea, BufferedWriter bw) {
        List<Area> areas = resolve(parentArea);
        areas.forEach(area -> {
            if (area.name.contains(",")) {
                throw new RuntimeException("不支持的区域名称（有英文逗号） " + area.name);
            }
            Util.bufferedWriterWrite(bw,
                    area.code + ',' + area.name + ',' + area.fullName + ',' + area.parent + '\n');
            if ((resolvedCount++ & 0x7F) == 0) {
                System.out.println("已处理 " + resolvedCount);
            }
        });
        areas.forEach(area -> {
            if (!area.leaf) {
                resolveAll(area, bw);
            }
        });
    }

    private static List<Area> resolve(Area parentArea) {
        while (true) {
            try {
                return resolveInternal(parentArea);
            } catch (SocketTimeoutException ex) {
                HTTP_CLIENT = new OkHttpClient();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    private static List<Area> resolveInternal(Area parentArea) throws IOException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2018");

        String parentCode = parentArea.code;

        int codeLen = parentCode.length();
        if (codeLen > 2) urlBuilder.append('/').append(parentCode, 0, 2);
        if (codeLen > 4) urlBuilder.append('/').append(parentCode, 2, 4);
        if (codeLen > 6) urlBuilder.append('/').append(parentCode, 4, 6);
        urlBuilder.append('/').append(parentCode).append(".html");

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(urlBuilder.toString());
        requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:62.0) Gecko/20100101 Firefox/62.0");
        requestBuilder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        requestBuilder.header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3");
        requestBuilder.cacheControl(CacheControl.FORCE_NETWORK);
        try (Response response = HTTP_CLIENT.newCall(requestBuilder.build()).execute()) {
            if (response == null || response.code() != 200 || response.body() == null) {
                throw new RuntimeException("Error response " + response);
            }

            byte[] bs = response.body().bytes();
            Document document = Jsoup.parse(new String(bs, "GBK"));
            Elements elements = document.getElementsByClass("citytable");
            if (elements.size() != 1) {
                elements = document.getElementsByClass("countytable");
                if (elements.size() != 1) {
                    elements = document.getElementsByClass("towntable");
                    if (elements.size() != 1) {
                        elements = document.getElementsByClass("villagetable");
                        if (elements.size() != 1) {
                            throw new RuntimeException();
                        }
                    }
                }
            }

            elements = elements.get(0).child(0).children();
            List<Area> areas = new ArrayList<>(elements.size());
            for (Element element : elements) {
                String className = element.className();
                if ("tr".equalsIgnoreCase(element.nodeName()) &&
                        ("citytr".equalsIgnoreCase(className)
                                || "countytr".equalsIgnoreCase(className)
                                || "towntr".equalsIgnoreCase(className)
                                || "villagetr".equalsIgnoreCase(className))) {
                    Elements tdElements = element.children();

                    int tdElementsSize = tdElements.size();
                    if (tdElementsSize != 2 && tdElementsSize != 3) {
                        throw new RuntimeException();
                    }
                    String areaId, areaName;
                    boolean leaf = true;
                    Element areaIdElement = tdElements.get(0);
                    Element areaNameElement = tdElements.get(tdElementsSize - 1);
                    if (areaIdElement.children().size() == 1 && "a".equalsIgnoreCase(areaIdElement.child(0).nodeName())) {
                        areaId = areaIdElement.child(0).text();
                        areaName = areaNameElement.child(0).text();
                        leaf = false;
                    } else if (areaIdElement.children().size() == 0) {
                        areaId = areaIdElement.text();
                        areaName = areaNameElement.text();
                    } else {
                        throw new RuntimeException();
                    }

                    areas.add(new Area(getShortAreaId(areaId), areaName, parentArea.fullName + '.' + areaName, parentCode, leaf));
                }
            }
            return areas;
        }
    }

    private static String getShortAreaId(String areaId) {
        if (areaId.length() != 12) {
            throw new RuntimeException(areaId);
        }
        if (areaId.endsWith("0000000000")) {
            return areaId.substring(0, 2);
        } else if (areaId.endsWith("00000000")) {
            return areaId.substring(0, 4);
        } else if (areaId.endsWith("000000")) {
            return areaId.substring(0, 6);
        } else if (areaId.endsWith("000")) {
            return areaId.substring(0, 9);
        } else {
            return areaId;
        }
    }

    private static class Area {
        private String code;
        private String name;
        private String fullName;
        private String parent;
        private boolean leaf;

        public Area(String code, String name, String fullName, String parent, boolean leaf) {
            this.code = code;
            this.name = name;
            this.fullName = fullName;
            this.parent = parent;
            this.leaf = leaf;
        }
    }
}


