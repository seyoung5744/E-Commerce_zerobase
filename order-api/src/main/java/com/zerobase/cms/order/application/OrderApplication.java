package com.zerobase.cms.order.application;

import static com.zerobase.cms.order.exception.ErrorCode.ORDER_FAIL_CHECK_CART;
import static com.zerobase.cms.order.exception.ErrorCode.ORDER_FAIL_NO_MONEY;

import com.zerobase.cms.order.client.UserClient;
import com.zerobase.cms.order.client.user.ChangeBalanceForm;
import com.zerobase.cms.order.client.user.CustomerDto;
import com.zerobase.cms.order.domain.model.ProductItem;
import com.zerobase.cms.order.domain.redis.Cart;
import com.zerobase.cms.order.exception.CustomException;
import com.zerobase.cms.order.service.ProductItemService;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderApplication {
    // 결제를 위해 필요한 것
    // 1번 : 물건들이 전부 주문 가능한 상태인지 확인
    // 2번 : 가격 변동이 있었는지에 대해 확인
    // 3번 : 고객의 돈이 충분한지.
    // 4번 : 결제 & 상품의 재고 관리

    private final CartApplication cartApplication;
    private final UserClient userClient;
    private final ProductItemService productItemService;


    @Transactional
    public void order(String token, Cart cart) {
        // 1. 주문 시 기존 카트 버림.
        // 2. 선택 주문 : 내가 사지 않은 아이템을 살려야함.

        Cart orderCart = cartApplication.refreshCart(cart);

        // 1번 : 물건들이 전부 주문 가능한 상태인지 확인
        // 2번 : 가격 변동이 있었는지에 대해 확인
        if (orderCart.getMessages().size() > 0) {
            // 문제가 있음.
            throw new CustomException(ORDER_FAIL_CHECK_CART);
        }

        // 3번 : 고객의 돈이 충분한지.
        CustomerDto customerDto = userClient.getCustomerInfo(token).getBody();

        int totalPrice = getTotalPrice(cart);
        if(customerDto.getBalance() < totalPrice){
            throw new CustomException(ORDER_FAIL_NO_MONEY);
        }

        // 금액 변경
        // TODO : 고객이 주문하려는 순간에 잔액 변경이되어서 주문되지 못하는 경우. rollback 전략을 고려해야한다.
        // 롤백 계획에 대해서 생각해야 함.
        userClient.changeBalance(token,
            ChangeBalanceForm.builder()
                .from("USER")
                .message("Order")
                .money(-totalPrice)
                .build());

        for(Cart.Product product : orderCart.getProducts()){
            for(Cart.ProductItem cartItem : product.getItems()){
                 ProductItem productItem = productItemService.getProductItem(cartItem.getId());
                 productItem.setCount(productItem.getCount() - cartItem.getCount());

            }
        }
    }

    private Integer getTotalPrice(Cart cart) {
        return cart.getProducts().stream().flatMapToInt(
            product -> product.getItems().stream().flatMapToInt(
                productItem -> IntStream.of(productItem.getPrice() * productItem.getCount()))
        ).sum();
    }
}
