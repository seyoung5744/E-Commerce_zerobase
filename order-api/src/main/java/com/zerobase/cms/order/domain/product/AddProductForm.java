package com.zerobase.cms.order.domain.product;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder // 본래 form에서는 getter까지만 필요하지만 테스트 코드를 위해 아래도 추가. 원래는 mocking을 해서 테스트를 짬
@NoArgsConstructor
@AllArgsConstructor
public class AddProductForm {

    private String name;
    private String description;
    private List<AddProductItemForm> items;
}
