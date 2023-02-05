package com.zerobase.cms.order.domain.redis;

import com.zerobase.cms.order.domain.product.AddProductCartForm;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

@Data
@NoArgsConstructor
@RedisHash("cart")
public class Cart {

    @Id
    private Long customerId;
    private List<Product> products = new ArrayList<>();
    private List<String> messages = new ArrayList<>();

    public void addMessage(String message){
        messages.add(message);
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product{
    // 변경 내용을 사용자에게 알려주기 위한 내부 static 클래스
        private Long id;
        private Long sellerId;
        private String name;
        private String description;
        private List<ProductItem> items = new ArrayList<>();

        public static Product from(AddProductCartForm form){
            return Product.builder()
                .id(form.getId())
                .sellerId(form.getSellerId())
                .name(form.getName())
                .description(form.getDescription())
                .items(form.getItems().stream().map(ProductItem::from).collect(Collectors.toList()))
                .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductItem{
        private Long id;
        private String name;
        private Integer count;
        private Integer price;

        public static ProductItem from(AddProductCartForm.ProductItem form){
            return ProductItem.builder()
                .id(form.getId())
                .name(form.getName())
                .count(form.getCount())
                .price(form.getPrice())
                .build();
        }
    }
}
