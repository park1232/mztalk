package com.mztalk.mentor.domain.dto;

import com.mztalk.mentor.domain.Status;
import com.mztalk.mentor.domain.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PaymentReqDto {

    private Long userId;
    private Long boardId;
    private int price;

    public Payment toEntity(){
        Payment payment = Payment.builder()
                .price(price)
                .status(Status.YES)
                .build();
        return payment;
    }


}
