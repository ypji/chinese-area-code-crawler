简单的爬虫用于爬取中国行政区域编码，从国家统计局（ http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2018/ ）获取数据。

本工具未做很好的封装，你需要将代码下载并导入IDE，直接在IDE中运行。

输出样例：

* [CSV数据](https://github.com/ypji/chinese-area-code-crawler/blob/master/examples/34.csv?raw=true)
* [SQL数据](https://github.com/ypji/chinese-area-code-crawler/blob/master/examples/34.sql?raw=true)

## 爬取数据

执行`exec.Executor`类。执行前修改`ROOT_AREA`和`TARGET_FILE`变量。

## 爬取的数据生成SQL

执行`exec.SQLGen`类，执行前修改`TARGET_FILE`和`SOURCE_FILE`变量。
