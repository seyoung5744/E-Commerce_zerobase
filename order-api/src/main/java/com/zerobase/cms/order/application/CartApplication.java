package com.zerobase.cms.order.application;

import static com.zerobase.cms.order.exception.ErrorCode.ITEM_COUNT_NOT_ENOUGH;
import static com.zerobase.cms.order.exception.ErrorCode.NOT_FOUND_PRODUCT;

import com.zerobase.cms.order.domain.model.Product;
import com.zerobase.cms.order.domain.model.ProductItem;
import com.zerobase.cms.order.domain.product.AddProductCartForm;
import com.zerobase.cms.order.domain.redis.Cart;
import com.zerobase.cms.order.exception.CustomException;
import com.zerobase.cms.order.service.CartService;
import com.zerobase.cms.order.service.ProductSearchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartApplication {

    private final ProductSearchService productSearchService;
    private final CartService cartService;

    public Cart addCart(Long customerId, AddProductCartForm form) {
        Product product = productSearchService.getByProductId(form.getId());

        if (product == null) {
            throw new CustomException(NOT_FOUND_PRODUCT);
        }

        Cart cart = cartService.getCart(customerId);

        if (cart != null && !addAble(cart, product, form)) {
            throw new CustomException(ITEM_COUNT_NOT_ENOUGH);
        }
        return cartService.addCart(customerId, form);
    }

    // 1. 장바구니에 상품을 추가 했다.
    // 2. 상품의 가격이나 수량이 변동 된다.
    public Cart getCart(Long customerId) {
        Cart cart = refreshCart(cartService.getCart(customerId));
        Cart returnCart = new Cart();
        returnCart.setCustomerId(customerId);
        returnCart.setProducts(cart.getProducts());
        returnCart.setMessages(cart.getMessages());
        cart.setMessages(new ArrayList<>());

        // 메세지 없는 것
        cartService.putCart(customerId, cart);
        return returnCart;

        // 2. 메세지를 보고난 다음에는, 이미 본 메세지는 스팸이 되기 때문에 제거한다.
    }

    public void clearCart(Long customerId){
        cartService.putCart(customerId, null);
    }

    private Cart refreshCart(Cart cart) {
        // 1. 상품이나 상품의 아이템의 정보, 가격, 수량이 변경되었는지 체크하고
        // 그에 맞는 알람 제공

        // 2. 상품의 수량, 가격을 우리가 임의로 변경한다.

        // 리스트로 작성해도 되지만 데이터가 많아지면 map 효율이 더 좋음
        Map<Long, Product> productMap = productSearchService.getListByProductIds(
            cart.getProducts().stream().map(Cart.Product::getId).collect(Collectors.toList()))
            .stream()
            .collect(Collectors.toMap(Product::getId, product -> product));

        // Iterator로 하면 내부에서 과정, 결과가 한번에 나와야하는데
        // 여기선 단순히 내부 정보만 변경할 것이기 때문에 성격이 다름.
        // 그래서 for문 이용
        for (int i = 0; i < cart.getProducts().size(); i++) {

            Cart.Product cartProduct = cart.getProducts().get(i);

            Product p = productMap.get(cartProduct.getId());

            if(p == null){
                cart.getProducts().remove(cartProduct);
                i--;
                cart.addMessage(cartProduct.getName() + " 상품이 삭제되었습니다.");
                continue;
            }


            Map<Long, ProductItem> productItemMap = p.getProductItems().stream()
                .collect(Collectors.toMap(ProductItem::getId, productItem -> productItem));

            List<String> tmpMessage = new ArrayList<>();
            // 각각 케이스 별로 에러를 쪼개고, 에러가 정상 출력 되야 하는지 체크해야 한다.
            for (int j = 0; j < cartProduct.getItems().size(); j++){
                Cart.ProductItem cartProductItem = cartProduct.getItems().get(j);
                ProductItem pi = productItemMap.get(cartProductItem.getId());

                if(pi == null){
                    cartProduct.getItems().remove(cartProductItem);
                    j--;
                    tmpMessage.add(cartProductItem.getName() + " 옵션이 삭제되었습니다.");
                    continue;
                }

                boolean isPriceChanged = false, isCountNotEnough = false;

                if (!cartProductItem.getPrice().equals(pi.getPrice())) {
                    isPriceChanged = true;
                    cartProductItem.setPrice(pi.getPrice());
                }

                if (cartProductItem.getCount() > pi.getCount()) {
                    isCountNotEnough = true;
                    cartProductItem.setCount(pi.getCount());
                }

                if (isPriceChanged && isCountNotEnough) {
                    // message 1
                    tmpMessage.add(cartProductItem.getName() + " 가격변동, 수량이 부족하여 구매 가능한 최대치로 변동되었습니다.");
                } else if (isPriceChanged) {
                    // message 2
                    tmpMessage.add(cartProductItem.getName() + " 가격이 변동되었습니다.");
                } else if (isCountNotEnough) {
                    // message 3
                    tmpMessage.add(cartProductItem.getName() + " 수량이 부족하여 구매 가능한 최대치로 변동되었습니다.");
                }
            }


            if(cartProduct.getItems().size() == 0){
                cart.getProducts().remove(cartProduct);
                i--;
                cart.addMessage(cartProduct.getName() + " 상품의 옵션이 모두 없어져 구매가 불가능합니다.");
            }
            else if(tmpMessage.size() > 0){
                StringBuilder builder = new StringBuilder();
                builder.append(cartProduct.getName() + " 상품의 변동 사항 : ");

                for(String message : tmpMessage){
                    builder.append(message);
                    builder.append(", ");
                }

                cart.addMessage(builder.toString());
            }
        }
        cartService.putCart(cart.getCustomerId(), cart);
        return cart;
    }

    private boolean addAble(Cart cart, Product product, AddProductCartForm form) {
//
        Cart.Product cartProduct = cart.getProducts().stream().filter(p -> p.getId().equals(form.getId()))
            .findFirst().orElse(Cart.Product.builder()
                .id(product.getId())
                .items(Collections.emptyList())
                .build());

        Map<Long, Integer> cartItemCountMap = cartProduct.getItems().stream()
            .collect(Collectors.toMap(Cart.ProductItem::getId, Cart.ProductItem::getCount));

        Map<Long, Integer> currentItemCountMap = product.getProductItems().stream()
            .collect(Collectors.toMap(ProductItem::getId, ProductItem::getCount));

        return form.getItems().stream().noneMatch(
            formItem -> {
                Integer cartCount = cartItemCountMap.get(formItem.getId());
                if(cartCount == null){
                    cartCount = 0;
                }
                Integer currentCount = currentItemCountMap.get(formItem.getId());
                return formItem.getCount() + cartCount > currentCount;
            });
    }
}
