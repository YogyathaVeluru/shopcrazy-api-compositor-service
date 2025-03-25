package com.scz.apicompservice;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OrderDatum {

    private String orderid;
    private String type;
    private String description;
    private String status;
    private String paymentid;
}
