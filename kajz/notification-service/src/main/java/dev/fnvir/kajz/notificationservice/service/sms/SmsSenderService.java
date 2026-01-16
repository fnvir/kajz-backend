package dev.fnvir.kajz.notificationservice.service.sms;

import java.util.Collection;

import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import dev.fnvir.kajz.notificationservice.config.TwilioConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsSenderService {

    private final TwilioConfig twilioConfig;

    @PostConstruct
    void init() {
        if (twilioConfig.isEnabled()) {
            Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
        } else {
            log.warn("Twilio is not enabled. SMS will not be sent!");
        }
    }

    public String sendSms(String to, String messageBody) {
        if (!twilioConfig.isEnabled()) {
            throw new IllegalStateException("Couldn't send sms: Twilio isn't enabled!");
        }
        
        if (to == null || !to.startsWith("+")) {
            log.warn("Couldn't send sms. Invalid phone number format: {}", to);
            return null;
        }
        
        Message message = Message.creator(
            new PhoneNumber(to),
            new PhoneNumber(twilioConfig.getPhoneNumber()),
            messageBody
        ).create();
        
        return message.getSid();
    }
    
    public Mono<String> sendSmsAsync(String to, String messageBody) {
        if (to == null || !to.startsWith("+")) {
            log.warn("Invalid phone number format (country code missing): {}", to);
            return Mono.empty();
        }

        return Mono.fromCallable(() -> sendSms(to, messageBody))
                   .subscribeOn(Schedulers.boundedElastic())
                   .doOnSuccess(sid -> log.debug("SMS sent to {} (SID: {})", to, sid))
                   .doOnError(e -> log.error("Failed to send SMS to {}: {}", to, e.getMessage()));
    }

    public Flux<String> sendBulkSmsAsync(Collection<String> recipients, String messageBody) {
        return Flux.fromIterable(recipients).flatMap(to -> sendSmsAsync(to, messageBody).onErrorResume(e -> {
            log.error("Failed to send SMS to {}: {}", to, e.getMessage());
            return Mono.empty(); // continue with next recipient
        }));
    }
    
}