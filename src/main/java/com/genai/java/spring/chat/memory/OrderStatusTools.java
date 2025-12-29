package com.genai.java.spring.chat.memory;

import com.genai.java.spring.chat.memory.dto.Order;
import com.genai.java.spring.chat.memory.dto.OrderStatus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderStatusTools {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        //Initialize 10 mock orders
        for (int i = 1; i <= 10; i++) {
            String orderId = "ORD-" + i;
            Order order = new Order(
                    orderId,
                    i % 2 == 0 ? "UPS" : "FedEx",
                    OrderStatus.CREATED,
                    "user-" + i,
                    "User " + i);
            orders.put(orderId, order);
        }
    }

    @Tool
    public String getOrderStatus(@ToolParam(description = "Order Id") String orderId, @ToolParam(description = "User Id") String userId) {
        Order order = orders.get(orderId);

        if (order != null && !order.userId().equals(userId)) {
            return "Order Id: " + orderId + " does not belong to User Id: " + userId;
        }

        if (order == null) {
            return "No order found for Id: " + orderId;
        }

        OrderStatus currentStatus = order.status();
        OrderStatus nextStatus = nextStage(currentStatus);

        Order updatedOrder = new Order(
                order.orderId(),
                order.carrier(),
                nextStatus,
                order.userId(),
                order.userName());

        orders.put(orderId, updatedOrder);

        return "Order " + orderId + " for " + order.userName() +
                " Carrier: " + order.carrier() + " is currently " + currentStatus;
    }

    private OrderStatus nextStage(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case CREATED -> OrderStatus.PROCESSING;
            case PROCESSING -> OrderStatus.SHIPPED;
            case SHIPPED -> OrderStatus.DELIVERED;
            case DELIVERED -> OrderStatus.DELIVERED; //stay at final stage
        };
    }
}
