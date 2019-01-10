/*
 * *
 *
 *     Created by OuYangX.
 *     Copyright (c) 2018, ouyangxian@gmail.com All Rights Reserved.
 *
 * /
 */

package com.business.model.event;

/**
 * 事件通道定义
 *
 * @author king
 * @date 2018-03-13
 */

public enum EventChannel {

    DB2HIVE("数据库传输 Hive 通道"),
    DB2HDFS("数据库传输 HDFS 通道"),
    DB2HBASE("数据库传输 HBASE 通道");

    String desc;

    EventChannel(String desc) {
        this.desc = desc;
    }
}
