package com.zerobase.cms.user.service.test;

import com.zerobase.cms.user.client.MailgunClient;
import com.zerobase.cms.user.client.mailgun.SendMailForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSendService {

    private final MailgunClient mailgunClient;

    public String sendEmail() {
        SendMailForm form = SendMailForm.builder()
            .from("seyoung574458@gmail.com")
            .to("seyoung5744@naver.com")
            .subject("Test smail from zero base")
            .text("my text")
            .build();

        return mailgunClient.sendEmail(form).getBody();
    }
}
