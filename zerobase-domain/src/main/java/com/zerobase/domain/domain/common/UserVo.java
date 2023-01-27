package com.zerobase.domain.domain.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

// id, email 두개를 저장해서 해당 값을 기반으로 동작
@AllArgsConstructor
@Getter
public class UserVo {

    private Long id;
    private String email;
}
