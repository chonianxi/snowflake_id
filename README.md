# snowflake_id
snowflake_id

snowflake_id是根据snowflake算法实现的一款分布式高可用ID生成器,
参考地址：https://github.com/twitter/snowflake


根据snowflake算法，其生成的ID是可配置的，自增长的，可用于大型系统的分库分表id的生成，并针对某些特定节假日可以方便的进行动态扩容，缩容。
目前主流的分布式/微服务对这方面的需求都存在，所以写了一个这个，欢迎大家参与维护，一起优化。


Linux下面优化 ulimit -n 655350

mvn install 打包编译到本地

服务默认绑定的是80端口，采用HTTP协议提供服务

启动顺序
1、安装zookeeper并启动服务
2、java -jar -Xmx4096m -Xms4096m -Xmn2g  id-center-1.0.0.jar 127.0.0.1:2181 1 1
id-center-1.0.0.jar是刚打的jar包,127.0.0.1:2181是刚安装的zookeeper的ip和连接端口 第一个1为数据中心ID(取值范围为0-31)，第二个1为生成ID的服务器编码（取值范围为0-15）
多网卡的情况下，需要绑定IP，启动可以指定IP，例如
java -jar -Xmx4096m -Xms4096m -Xmn2g  id-center-1.0.0.jar 127.0.0.1:2181 192.168.1.133 1 1


本机测试
本地运行服务，在windows下面，没有做任何优化，配置如下
CPU I5-6300HQ
内存8G

用一台同样的机器过来做压力测试，TPS保持在2-3W之间；测试服务器CPU100%，ID提供服务器没压力，CPU60-70%照常可以浏览网页，撸代码。预计到Linux环境下，经过优化，单台4核心8G的配置，最少可以做到30-50W的TPS


