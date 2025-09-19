package com.C_platform.order.application.dto;

public record CreateOrderCommand(
        Long buyerId,
        Long itemId,
        Long addressId,
        String detailMessage)
{ }
