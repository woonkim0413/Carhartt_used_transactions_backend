package com.C_platform.payment.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Table(name = "payment")
//@Entity
@Builder(access = AccessLevel.PRIVATE)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
}
