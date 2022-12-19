package com.mztalk.main.domain.follow.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FollowDto {

    private Long id;
    private String username;
    private BigInteger followState;   //상태여부 boolean
    private BigInteger  equalUserState; //동일인여부


}
