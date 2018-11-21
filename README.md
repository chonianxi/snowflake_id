# snowflake_id
snowflake_id

snowflake_id是根据snowflake算法实现的一款分布式高可用ID生成器,
参考地址：https://github.com/twitter/snowflake


根据snowflake算法，其生成的ID是可配置的，自增长的，可用于大型系统的分库分表id的生成，并针对某些特定节假日可以方便的进行动态扩容，缩容。
目前主流的分布式/微服务对这方面的需求都存在，所以写了一个这个，欢迎大家参与维护，一起优化。
