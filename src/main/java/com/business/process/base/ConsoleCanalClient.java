/*
 * *
 *
 *     Created by OuYangX.
 *     Copyright (c) 2018, ouyangxian@gmail.com All Rights Reserved.
 *
 * /
 */

package com.business.process.base;

/**
 *
 * Canal kafka client
 * @author ouyangxian
 *
 */

public class ConsoleCanalClient extends AbstractCanalClient {


    public ConsoleCanalClient(String destination) {
        super(destination);
    }


    public static void main(String[] args) {

        final ConsoleCanalClient kafkaCanalClient = new ConsoleCanalClient("InvestDataSource");
        kafkaCanalClient.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    logger.info("## stop the canal kafka client");
                    kafkaCanalClient.stop();
                } catch (Throwable e) {
                    logger.warn("##something goes wrong when stopping canal:", e);
                } finally {
                    logger.info("## canal kafka client is down.");
                }
            }

        });
    }

}
