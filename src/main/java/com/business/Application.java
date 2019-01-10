/*
 * *
 *
 *     Created by OuYangX.
 *     Copyright (c) 2018, ouyangxian@gmail.com All Rights Reserved.
 *
 * /
 */

package com.business;

import com.zyxr.bi.business.process.KafkaCanalClient;
import com.zyxr.bi.business.util.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * binlog 数据通道程序入口
 *
 * @author: OuYangX
 * @date: 2018/1/26 下午2:19
 **/

public class Application {

    protected final static Logger logger = LoggerFactory.getLogger(Application.class);


    public static void main(String[] args) {

        String destinationText = SystemConfig.getProperty("destination");

        if (StringUtils.isEmpty(destinationText)) {
            logger.warn("Start failed, destination empty!");
            logger.warn("Please set destination values to conf/application.properties file.");
            System.exit(0);
        }

        List<String> destinations = Arrays.asList(destinationText.split(","));

       final List<KafkaCanalClient> kafkaCanalClients = destinations.stream()
            .map(KafkaCanalClient::new)
            .collect(Collectors.toList());

        Runtime.getRuntime().addShutdownHook(new ShutdownHookListening(kafkaCanalClients));

        kafkaCanalClients.parallelStream().forEach(KafkaCanalClient::start);

        logger.info("Oook!{} process started.", destinationText);
    }

    static class ShutdownHookListening extends Thread {
        List<KafkaCanalClient> kafkaCanalClients;

        public ShutdownHookListening(List<KafkaCanalClient> kafkaCanalClients) {
            this.kafkaCanalClients = kafkaCanalClients;
        }

        @Override
        public void run() {
            try {
                kafkaCanalClients.forEach(KafkaCanalClient::stop);
            } catch (Throwable e) {
                logger.warn("##something goes wrong when stopping data-channel:", e);
            } finally {
                logger.info("##data-channel is down.");
            }
        }
    }
}
