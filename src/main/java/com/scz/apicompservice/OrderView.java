package com.scz.apicompservice;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter @Setter
public class OrderView {

    private String orderid;
    private String paymentid;
    private String status;

}
