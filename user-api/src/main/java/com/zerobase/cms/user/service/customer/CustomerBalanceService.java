package com.zerobase.cms.user.service.customer;

import static com.zerobase.cms.user.exception.ErrorCode.NOT_ENOUGH_BALANCE;
import static com.zerobase.cms.user.exception.ErrorCode.NOT_FOUND_USER;

import com.zerobase.cms.user.domain.customer.ChangeBalanceForm;
import com.zerobase.cms.user.domain.model.CustomerBalanceHistory;
import com.zerobase.cms.user.domain.repository.CustomerBalanceHistoryRepository;
import com.zerobase.cms.user.domain.repository.CustomerRepository;
import com.zerobase.cms.user.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerBalanceService {

    private final CustomerBalanceHistoryRepository customerBalanceHistoryRepository;
    private final CustomerRepository customerRepository;

    private static final Integer MINUS_BALANCE_STANDARD = 0;

    @Transactional(noRollbackFor = {CustomException.class})
    public CustomerBalanceHistory changeBalance(Long customerId, ChangeBalanceForm form) throws CustomException {
        CustomerBalanceHistory customerBalanceHistory =
            customerBalanceHistoryRepository.findFirstByCustomer_IdOrderByIdDesc(customerId) // 최근 결제 내역
                .orElse(CustomerBalanceHistory.builder() // 결제 내역이 없다면
                    .changeMoney(0)
                    .currentMoney(0)
                    .customer(customerRepository.findById(customerId)
                        .orElseThrow(() -> new CustomException(NOT_FOUND_USER)))
                    .build());

        if(customerBalanceHistory.getCurrentMoney() + form.getMoney() < MINUS_BALANCE_STANDARD){
            throw new CustomException(NOT_ENOUGH_BALANCE);
        }

        customerBalanceHistory = CustomerBalanceHistory.builder()
            .changeMoney(form.getMoney())
            .currentMoney(customerBalanceHistory.getCurrentMoney() + form.getMoney())
            .description(form.getMessage())
            .fromMessage(form.getFrom())
            .customer(customerBalanceHistory.getCustomer())
            .build();

        // customer에 잔액을 만들어 놓는 이유는 매번 join해서 가져와야하는 문제가 있음. 명시적으로 매번 join해서 가져오는 것은 좋지 않음.
        customerBalanceHistory.getCustomer().setBalance(customerBalanceHistory.getCurrentMoney());

        return customerBalanceHistoryRepository.save(customerBalanceHistory);
    }

}
