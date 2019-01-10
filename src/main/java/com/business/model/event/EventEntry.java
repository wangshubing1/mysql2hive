/*
 * *
 *
 *     Created by OuYangX.
 *     Copyright (c) 2018, ouyangxian@gmail.com All Rights Reserved.
 *
 * /
 */

package com.business.model.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.Collections;
import java.util.List;

/**
 * 事件条目对象
 *
 * @author ouyangxian
 * @date 2018-03-13
 */

public class EventEntry<T> {

    private String eventId;

    private String eventChannel;

    private EventType eventType;

    private Long eventTime;

    // 事件数据，存储的目标，
    // 如果 hive 为 schemeName.tableName
    // 如果 hdfs 为 /user/xxx/path

    private String eventTarget;

    private List<T> eventData;

    public String getEventChannel() {
        return eventChannel;
    }

    public void setEventChannel(String eventChannel) {
        this.eventChannel = eventChannel;
    }

    public String getEventTarget() {
        return eventTarget;
    }

    public void setEventTarget(String eventTarget) {
        this.eventTarget = eventTarget;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Long getEventTime() {
        return eventTime;
    }

    public void setEventTime(Long eventTime) {
        this.eventTime = eventTime;
    }

    public List<T> getEventData() {
        return eventData;
    }

    public void setEventData(List<T> eventData) {
        this.eventData = eventData;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public static void main(String[] args) {

        EventEntry<JSONObject> eventEntry = new EventEntry();
        eventEntry.setEventId("00000010000123231");
        eventEntry.setEventChannel(EventChannel.DB2HIVE.name());
        eventEntry.setEventType(EventType.INSERT);
        eventEntry.setEventTime(System.currentTimeMillis());
        eventEntry.setEventTarget("testdb.test_table");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", 10001);
        jsonObject.put("name", "testName");
        eventEntry.setEventData(Collections.singletonList(jsonObject));
        System.out.println(eventEntry);

        EventEntry eventEntry1 = JSON.parseObject(eventEntry.toString(), EventEntry.class);
        System.out.println(eventEntry1);
    }
}
