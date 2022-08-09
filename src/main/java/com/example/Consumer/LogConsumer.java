package com.example.Consumer;

import com.alibaba.fastjson.JSONObject;
import com.example.Pojo.Log;
import com.example.Service.LogDaoService;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener (
        consumerGroup = "logconsumer",
        topic = "log",
        selectorExpression = "正常 || 轻微 || 一般 || 严重 || 非常严重"
)
public class LogConsumer implements RocketMQListener<MessageExt> {
    private final Logger log = LoggerFactory.getLogger(LogConsumer.class);
    private final LogDaoService logDevelopDaoService;


    @Autowired
    public LogConsumer(LogDaoService logDevelopDaoService) {
        this.logDevelopDaoService = logDevelopDaoService;
    }

    @Override
    public void onMessage(MessageExt status) {
        log.debug("消费者从MQ获取成功消息 : " + status);
        try {
            String tag = status.getTags();
            if (tag.equals("正常")) // 正常
                this.trace(status);

            if (tag.equals("轻微")) // 轻微
                this.debug(status);

            if (tag.equals("一般")) // 一般
                this.info(status);

            if (tag.equals("严重")) //
                this.warn(status);

            if (tag.equals(" 非常严重")) // 严重
                this.error(status);

//            if (tag.equals("fatal")) // 非常严重
//                this.fatal(status);
        } catch (Exception e) {
            // 异常直接回滚
            e.printStackTrace();
        }
    }


    private void trace(MessageExt status){
        String data = new String(status.getBody());
        Log logOperation = JSONObject.parseObject(data, Log.class);
        this.logDevelopDaoService.InsertDB(logOperation);
    }

    private void debug(MessageExt status){
        this.trace(status);
    }

    private void info(MessageExt status){
        this.trace(status);
    }

    private void warn(MessageExt status){
        this.trace(status);
    }

    private void error(MessageExt status){
        this.trace(status);
    }

    private void fatal(MessageExt status){
        this.trace(status);
//        this.logDevelopDaoService.Emails();
    }



}
