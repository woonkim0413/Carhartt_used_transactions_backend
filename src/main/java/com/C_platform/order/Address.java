package com.C_platform.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter @Setter
public class Address {
    private String city;
    private String street;
    private String home_number;

    public Address(String city, String street, String home_number) {
        this.city = city;
        this.street = street;
        this.home_number = home_number;
    }
}