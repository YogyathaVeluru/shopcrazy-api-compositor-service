package com.scz.apicompservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("shopcrazy/api/v1")
public class MainRestController {

    private static final Logger log = LoggerFactory.getLogger(MainRestController.class);

    //@Autowired
    //OrderRepository orderRepository;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    Producer producer;

    @Autowired
    @Qualifier("order-service-get-order")
    WebClient webClientOrderService;

    @Autowired
    @Qualifier("payment-service-get-payment")
    WebClient webClientPymntService;

    @Autowired
    AuthService authService;

    @GetMapping("get/order/{orderid}")
    public ResponseEntity<?> getOrder(@PathVariable String orderid,
                                      @RequestHeader("Authorization") String token,
                                      HttpServletRequest request,
                                      HttpServletResponse response) throws JsonProcessingException {
        // COOKIE VALIDATION LOGIC
        List<Cookie> cookieList = null;
        log.info("initiating cookie check");

        //Optional<String> healthStatusCookie = Optional.ofNullable(request.getHeader("health_status_cookie"));
        Cookie[] cookies = request.getCookies();
        if(cookies == null)
        {
            cookieList = new ArrayList<>();
        }
        else
        {
            // REFACTOR TO TAKE NULL VALUES INTO ACCOUNT
            cookieList = List.of(cookies);
        }
        log.info("cookie check complete");

        log.info("Setting up the Cookie for the Front-end");
        Cookie cookieStage1 = new Cookie("api-comp-order-1", orderid);
        cookieStage1.setMaxAge(300);
        log.info("Cookie set up successfully");
        redisTemplate.opsForValue().set(cookieStage1.getValue(), "fresh !");

        if( cookieList.stream().filter(cookie -> cookie.getName().equals("api-comp-order-1")).findAny().isEmpty()) // COOKIE_CHECK
        {
            if(authService.validateToken(token))
            {
                Order order =  webClientOrderService.get().uri("/"+orderid).retrieve().bodyToMono(Order.class).block();
                Payment payment = webClientPymntService.get().uri("/"+orderid).retrieve().bodyToMono(Payment.class).block();

                if (order != null && payment != null)
                {
                    OrderView orderView = new OrderView();
                    orderView.setOrderid(orderid);
                    orderView.setPaymentid(payment.getPaymentid());
                    orderView.setStatus(order.getStatus());
                    response.addCookie(cookieStage1);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String orderViewJson =  objectMapper.writeValueAsString(orderView);
                    redisTemplate.opsForValue().set(cookieStage1.getValue(), "fetched "+orderViewJson);
                    return ResponseEntity.ok(orderView);
                }
                else if (order == null)
                {
                    response.addCookie(cookieStage1);
                    return ResponseEntity.ok("Order not found");
                }
                else if (payment == null)
                {
                    OrderView orderView = new OrderView();
                    orderView.setOrderid(orderid);
                    //orderView.setPaymentid(payment.getPaymentid());
                    orderView.setStatus(order.getStatus());
                    response.addCookie(cookieStage1);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String orderViewJson =  objectMapper.writeValueAsString(orderView);
                    redisTemplate.opsForValue().set(cookieStage1.getValue(), "fetched "+orderViewJson);

                    return ResponseEntity.ok(orderView);
                }
                else
                {
                    response.addCookie(cookieStage1);
                    return ResponseEntity.ok("Order and Payment not found");
                }
            }
            else
            {
                return ResponseEntity.ok("Invalid token");
            }



        }else
        {
            // FOLLOW UP LOGIC
            log.info("found a relevant cookie.. initiating follow up logic");

            Cookie followup_cookie =  cookieList.stream().
                    filter(cookie -> cookie.getName().equals("api-comp-order-1")).findAny().get();

            String followup_cookie_key = followup_cookie.getValue();
            String cacheResponse = (String)redisTemplate.opsForValue().get(followup_cookie_key);

            String[] cacheResponseArray = cacheResponse.split(" ");

            if(cacheResponseArray[0].equals("fresh"))
            {
                log.info("Request still under process...");
                return ResponseEntity.ok("Request still under process...");
            }
            else if(cacheResponseArray[0].equals("fetched"))
            {
                log.info("Request already processed");
                ObjectMapper objectMapper = new ObjectMapper();
                OrderView orderView = objectMapper.readValue(cacheResponseArray[1], OrderView.class);
                return ResponseEntity.ok(orderView);
            }
            else
            {
                return ResponseEntity.ok("Error Processing the Order");
            }
        }


    }

}
