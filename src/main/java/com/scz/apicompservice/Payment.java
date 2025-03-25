package com.scz.apicompservice;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Payment {

    private String paymentid;


    private String orderid;


    private Integer amount;


    private String status;

}