package com.zerobase.cms.user.service.customer;

import static com.zerobase.cms.user.exception.ErrorCode.ALREADY_VERIFY;
import static com.zerobase.cms.user.exception.ErrorCode.EXPIRE_CODE;
import static com.zerobase.cms.user.exception.ErrorCode.NOT_FOUND_USER;
import static com.zerobase.cms.user.exception.ErrorCode.WRONG_VERIFICATION;

import com.zerobase.cms.user.domain.SignUpForm;
import com.zerobase.cms.user.domain.model.Customer;
import com.zerobase.cms.user.domain.repository.CustomerRepository;
import com.zerobase.cms.user.exception.CustomException;
import com.zerobase.cms.user.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignUpCustomerService {

    private final CustomerRepository customerRepository;

    public Customer signUp(SignUpForm form){
        return customerRepository.save(Customer.from(form));
    }

    public boolean isEmailExist(String email){
        return customerRepository.findByEmail(email.toLowerCase(Locale.ROOT))
            .isPresent();
    }

    @Transactional
    public void verifyEmail(String email, String code){
        Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

        if(customer.isVerify()){
            throw new CustomException(ALREADY_VERIFY);
        }

        if(!customer.getVerificationCode().equals(code)){
            throw new CustomException(WRONG_VERIFICATION);
        }

        if(customer.getVerifyExpiredAt().isBefore(LocalDateTime.now())){
            throw new CustomException(EXPIRE_CODE);
        }

        customer.setVerify(true);

    }

    @Transactional
    public LocalDateTime changeCustomerValidateEmail(Long customerId, String verificationCode){
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

        customer.setVerificationCode(verificationCode);
        customer.setVerifyExpiredAt(LocalDateTime.now().plusDays(1));

        return customer.getVerifyExpiredAt();
    }
}
