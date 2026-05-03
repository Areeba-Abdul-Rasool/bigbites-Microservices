package com.bigbites.productService.kafka;

public class KafkaTopics {
    public static final String ORDER_PLACED         = "order.placed";
    public static final String ORDER_STATUS_UPDATED = "order.status.updated";
    public static final String ORDER_CANCELLED      = "order.cancelled";

    public static final String PAYMENT_SUCCESS      = "payment.success";
    public static final String PAYMENT_FAILED       = "payment.failed";
    public static final String PAYMENT_REFUNDED     = "payment.refunded";

    public static final String LOW_STOCK_ALERT      = "product.low.stock";
    public static final String STOCK_DEDUCTED       = "product.stock.deducted";
}