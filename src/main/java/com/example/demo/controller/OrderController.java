package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
    OrderService orderService;
    OrderController(OrderService orderService){
        this.orderService = orderService;
    }

    @PostMapping("/place/order")
    public String placeOrder(@RequestBody Order order){
        orderService.createOrder(order);
        System.out.println("Order sent successfully");
        return "Order sent successfully";
    }
}
