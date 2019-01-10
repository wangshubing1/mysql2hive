/*
 * *
 *
 *     Created by OuYangX.
 *     Copyright (c) 2018, ouyangxian@gmail.com All Rights Reserved.
 *
 * /
 */

package com.business.process.base;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry.*;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import com.business.model.event.EventEntry;
import com.until.SystemConfig;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 消费基类
 *
 * @author ouyangxian
 */
public abstract class AbstractCanalClient implements Serializable {

    protected final static Logger logger = LoggerFactory.getLogger(AbstractCanalClient.class);
    protected static final String SEP = SystemUtils.LINE_SEPARATOR;
    protected static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String SPLIT_TABLE_REGEX = ".*_\\d{2,3}$";
    public static final String SPLIT_SPEC = "_";

    protected volatile boolean running = false;
    protected Thread.UncaughtExceptionHandler handler = (t, e) -> logger.error("parse events has an error", e);
    protected Thread thread = null;
    protected CanalConnector connector;
    protected static String context_format = null;
    protected static String row_format = null;
    protected static String transaction_format = null;

    protected String destination;
    protected String clientModel = "simple";
    protected String ip = "127.0.0.1";
    protected int port = 11111;
    protected String zkAddress = "";
    protected String username = "";
    protected String password = "";
    protected String filter = "";


    static {
        context_format = SEP + "****************************************************" + SEP;
        context_format += "* Batch Id: [{}] ,count : [{}] , memsize : [{}] , Time : {}" + SEP;
        context_format += "* Start : [{}] " + SEP;
        context_format += "* End : [{}] " + SEP;
        context_format += "****************************************************" + SEP;

        row_format = SEP
                + "----------------> binlog[{}:{}] , name[{},{}] , eventType : {} , executeTime : {} , delay : {}ms"
                + SEP;

        transaction_format = SEP + "================> binlog[{}:{}] , executeTime : {} , delay : {}ms" + SEP;

    }

    public AbstractCanalClient(String destination) {
        this(destination, null);
    }

    public AbstractCanalClient(String destination, CanalConnector connector) {
        this.destination = destination;
        this.clientModel = SystemConfig.getProperty(destination.concat(".").concat("client.model"), "simple");
        this.ip = SystemConfig.getProperty(destination.concat(".").concat("ip"));
        this.port = SystemConfig.getIntProperty(destination.concat(".").concat("port"));
        this.zkAddress = SystemConfig.getProperty(destination.concat(".").concat("zk.address"));
        this.username = SystemConfig.getProperty(destination.concat(".").concat("username"));
        this.password = SystemConfig.getProperty(destination.concat(".").concat("password"));
        this.filter = SystemConfig.getProperty(destination.concat(".").concat("filter"));

        if (connector == null) {
            this.connector = "simple".equals(clientModel)
                    ? CanalConnectors.newSingleConnector(new InetSocketAddress(ip, port),
                    destination,
                    username,
                    password)
                    : CanalConnectors.newClusterConnector(zkAddress,
                    destination,
                    username,
                    password);
        } else {
            this.connector = connector;

        }
    }

    public void start() {
        Assert.notNull(connector, "connector is null");
        thread = new Thread(() -> process(), destination.concat("-Thread"));

        thread.setUncaughtExceptionHandler(handler);
        thread.start();
        running = true;
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }

        MDC.remove("destination");
    }

    protected void prepare() {
    }

    protected void destroy() {
    }

    protected void process() {
        int batchSize = 1024 * 5;
        while (running) {
            try {
                MDC.put("destination", destination);
                connector.connect();
                connector.subscribe();
                prepare();

                while (running) {
                    Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                    long batchId = message.getId();
                    int size = message.getEntries().size();
                    if (batchId == -1 || size == 0) {

                    } else {
                        printSummary(message, batchId, size);
                        makeEntrys(message.getEntries());
                    }

                    connector.ack(batchId); // 提交确认
                    // connector.rollback(batchId); // 处理失败, 回滚数据
                }
            } catch (Exception e) {
                logger.error("process error!", e);
            } finally {
                connector.disconnect();
                MDC.remove("destination");
                destroy();
            }
        }
    }

    protected void printSummary(Message message, long batchId, int size) {
        long memsize = 0;
        for (Entry entry : message.getEntries()) {
            memsize += entry.getHeader().getEventLength();
        }

        String startPosition = null;
        String endPosition = null;
        if (!CollectionUtils.isEmpty(message.getEntries())) {
            startPosition = buildPositionForDump(message.getEntries().get(0));
            endPosition = buildPositionForDump(message.getEntries().get(message.getEntries().size() - 1));
        }

        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        logger.info(context_format, new Object[]{batchId, size, memsize, format.format(new Date()), startPosition,
                endPosition});
    }

    protected String buildPositionForDump(Entry entry) {
        long time = entry.getHeader().getExecuteTime();
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        return entry.getHeader().getLogfileName() + ":" + entry.getHeader().getLogfileOffset() + ":"
                + entry.getHeader().getExecuteTime() + "(" + format.format(date) + ")";
    }

    protected void makeEntrys(List<Entry> entrys) {

        for (Entry entry : entrys) {
            long executeTime = entry.getHeader().getExecuteTime();
            long delayTime = System.currentTimeMillis() - executeTime;

            if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND) {
                if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN) {
                    TransactionBegin begin = null;
                    try {
                        begin = TransactionBegin.parseFrom(entry.getStoreValue());
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException("parse event has an error , data:" + entry.toString(), e);
                    }
                    // 打印事务头信息，执行的线程id，事务耗时
                    logger.info(transaction_format,
                            new Object[]{entry.getHeader().getLogfileName(),
                                    String.valueOf(entry.getHeader().getLogfileOffset()),
                                    String.valueOf(entry.getHeader().getExecuteTime()), String.valueOf(delayTime)});
                    logger.info(" BEGIN ----> Thread id: {}", begin.getThreadId());
                } else if (entry.getEntryType() == EntryType.TRANSACTIONEND) {
                    TransactionEnd end = null;
                    try {
                        end = TransactionEnd.parseFrom(entry.getStoreValue());
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException("parse event has an error , data:" + entry.toString(), e);
                    }
                    // 打印事务提交信息，事务id
                    logger.info(" END ----> transaction id: {}", end.getTransactionId());
                    logger.info(transaction_format,
                            new Object[]{entry.getHeader().getLogfileName(),
                                    String.valueOf(entry.getHeader().getLogfileOffset()),
                                    String.valueOf(entry.getHeader().getExecuteTime()), String.valueOf(delayTime)});
                }

                continue;
            }

            if (entry.getEntryType() == EntryType.ROWDATA) {
                RowChange rowChage = null;
                try {
                    rowChage = RowChange.parseFrom(entry.getStoreValue());
                } catch (Exception e) {
                    throw new RuntimeException("parse event has an error , data:" + entry.toString(), e);
                }

                EventType eventType = rowChage.getEventType();

                logger.info(row_format,
                        new Object[]{entry.getHeader().getLogfileName(),
                                String.valueOf(entry.getHeader().getLogfileOffset()), entry.getHeader().getSchemaName(),
                                entry.getHeader().getTableName(), eventType,
                                String.valueOf(entry.getHeader().getExecuteTime()), String.valueOf(delayTime)});

                if (eventType == EventType.QUERY || rowChage.getIsDdl()) {
                    logger.info(" sql ----> " + rowChage.getSql() + SEP);
                    continue;
                }

                String tableName = entry.getHeader().getTableName();

                for (RowData rowData : rowChage.getRowDatasList()) {

                    EventEntry<JSONObject> eventEntry = new EventEntry();

                    eventEntry.setEventId(entry.getHeader().getLogfileName()
                            .concat(String.valueOf(entry.getHeader().getLogfileOffset())));
                    eventEntry.setEventTarget(entry.getHeader().getSchemaName().concat(".")
                            .concat(!Pattern.matches(SPLIT_TABLE_REGEX, tableName)
                                    ? tableName
                                    : tableName.substring(0, tableName.lastIndexOf(SPLIT_SPEC))));

                    if (eventType == EventType.INSERT) {
                        eventEntry.setEventType(com.business.model.event.EventType.INSERT);
                        eventEntry.setEventData(Collections.singletonList(makeColumn(rowData.getAfterColumnsList())));

                    } else if (eventType == EventType.DELETE) {
                        eventEntry.setEventType(com.business.model.event.EventType.DELETE);
                        eventEntry.setEventData(Collections.singletonList(makeColumn(rowData.getBeforeColumnsList())));
                    } else {
                        eventEntry.setEventType(com.business.model.event.EventType.UPDATE);
                        eventEntry.setEventData(Collections.singletonList(makeColumn(rowData.getAfterColumnsList())));
                    }

                    after(eventEntry);
                }
            }
        }
    }

    protected JSONObject makeColumn(List<Column> columns) {
        JSONObject columnMap = new JSONObject();
        for (Column column : columns) {
            if(column.getIsKey()) {
                columnMap.put("_pk", column.getName());
            }
            columnMap.put(column.getName(), column.getValue());
        }
        return columnMap;
    }

    protected void after(EventEntry data) {
        logger.debug("default after");
    }

}
