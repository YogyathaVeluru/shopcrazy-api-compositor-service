package com.scz.apicompservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class Consumer
{
    @Autowired
    RedisTemplate<String, String> redisTemplate;
    private final Logger logger = LoggerFactory.getLogger(Consumer.class);

    @KafkaListener(topics = "order-events", groupId = "order-events-consumer-api-comp-service")
    public void consumeAuthEvents(String message) throws IOException
    {
        logger.info("DOMAIN EVENT RECEIVED "+message);

        //analytics_counter.increment();
        ObjectMapper mapper  = new ObjectMapper();
        OrderDatum datum =  mapper.readValue(message, OrderDatum.class);

        OrderView orderView = new OrderView();

        orderView.setOrderid(datum.getOrderid());
        orderView.setPaymentid(datum.getPaymentid());
        orderView.setStatus(datum.getStatus());

        logger.info("Cache Refreshing");
        ObjectMapper objectMapper = new ObjectMapper();
        String orderViewJson =  objectMapper.writeValueAsString(orderView);

        logger.info("orderid: "+orderView.getOrderid());

        redisTemplate.opsForValue().set(orderView.getOrderid(), "fetched "+orderViewJson);

        //analyticRepository.save(datum);
        logger.info("Cache Refreshed");
    }




}

