package com.zerobase.cms.user.client;

import com.zerobase.cms.user.client.mailgun.SendMailForm;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "mailgun", url = "https://api.mailgun.net/v3/")
@Qualifier("mailgun") // 여러 개의 FeignClient를 사용하기 때문에 Qualifier로 구분
public interface MailgunClient {

    @PostMapping("sandbox03d8c9b8dbf649c39f32d5579546a5fd.mailgun.org/messages")
    ResponseEntity<String> sendEmail(@SpringQueryMap SendMailForm form);

}
