package dev.fnvir.kajz.authservice.service;

import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {
    
    private final Cache<String, String> otpCache;
    private final StringRedisTemplate redisTemplate;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration DEFAULT_OTP_TTL = Duration.ofMinutes(6);

    public static enum OtpType {

        EMAIL_VERFICATION("email_verify"),
        PASSWORD_RECOVERY("reset_pass");

        private final String prefix;

        OtpType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
        
        public String toString() {
            return getPrefix();
        }
    }

    /**
     * Generate a random 6-digit OTP and save it using the given prefix.
     * 
     * @return the generated OTP.
     */
    public String generateAndSaveOtp(OtpType prefix, String identifier) {
        String otp = RANDOM.nextInt(100000, 1_000_000)+""; // 6-digits
        saveOtp(prefix, identifier, otp);
        return otp;
    }

    /** 
     * Verify the OTP and delete it from cache only if it matches.
     */
    public boolean verifyOtp(OtpType prefix, String identifier, String inputOtp) {
        String key = buildKey(prefix, identifier);

        // first check local cache
        String caffeineOtp = otpCache.getIfPresent(key);
        if (caffeineOtp != null && caffeineOtp.equalsIgnoreCase(inputOtp)) {
            deleteOtp(key);
            return true;
        }

        // then check distributed cache
        String redisOtp = redisTemplate.opsForValue().get(key);
        if (redisOtp != null && redisOtp.equalsIgnoreCase(inputOtp)) {
            deleteOtp(key);
            return true;
        }

        return false;
    }
    
    private void saveOtp(OtpType otpType, String identifier, String otp) {
        String key = buildKey(otpType, identifier);
        otpCache.put(key, otp);
        redisTemplate.opsForValue().set(key, otp, DEFAULT_OTP_TTL);
    }

    private String buildKey(OtpType otpType, String identifier) {
        return "otp:" + otpType.getPrefix() + ":" + identifier;
    }
    
    private void deleteOtp(String key) {
        otpCache.invalidate(key);
        redisTemplate.delete(key);
    }

}