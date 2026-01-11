package dev.fnvir.kajz.authservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.github.benmanes.caffeine.cache.Cache;

import dev.fnvir.kajz.authservice.service.OtpService.OtpType;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Unit Tests")
class OtpServiceTest {
    
    @Mock
    private Cache<String, String> otpCache;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private OtpService otpService;

    private ValueOperations<String, String> valueOperations;
    
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_OTP = "123456";

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        valueOperations = mock(ValueOperations.class);
    }
    
    
    @Nested
    @DisplayName("generateAndSaveOtp tests")
    class GenerateAndSaveOtpTests {
        
      @Captor
      private ArgumentCaptor<String> keyCaptor;

        @Test
        @DisplayName("Should generate a 6-digit OTP")
        void shouldGenerateSixDigitOtp() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            String otp = otpService.generateAndSaveOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL);

            assertEquals(otp.length(), 6);
            assertTrue(otp.matches("\\d{6}"));
        }

        @Test
        @DisplayName("Should save OTP to both local cache and Redis")
        void shouldSaveOtpToBothCaches() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            String otp = otpService.generateAndSaveOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL);

            String expectedKey = "otp:email_verify:" + TEST_EMAIL;
            verify(otpCache).put(eq(expectedKey), eq(otp));
            verify(valueOperations).set(eq(expectedKey), eq(otp), eq(Duration.ofMinutes(6)));
        }

        @Test
        @DisplayName("Should use correct key prefix for EMAIL_VERIFICATION type")
        void shouldUseCorrectKeyPrefixForEmailVerification() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            otpService.generateAndSaveOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL);

            verify(otpCache).put(keyCaptor.capture(), anyString());
            assertTrue(keyCaptor.getValue().startsWith("otp:email_verify:"));
        }

        @Test
        @DisplayName("Should use correct key prefix for PASSWORD_RECOVERY type")
        void shouldUseCorrectKeyPrefixForPasswordRecovery() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            otpService.generateAndSaveOtp(OtpType.PASSWORD_RECOVERY, TEST_EMAIL);

            verify(otpCache).put(keyCaptor.capture(), anyString());
            assertTrue(keyCaptor.getValue().startsWith("otp:reset_pass:"));
        }
    }

    @Nested
    @DisplayName("verifyOtp tests")
    class VerifyOtpTests {

        @Test
        @DisplayName("Should return true when OTP matches in local cache")
        void shouldReturnTrueWhenOtpMatchesInLocalCache() {
            String key = "otp:email_verify:" + TEST_EMAIL;
            when(otpCache.getIfPresent(key)).thenReturn(TEST_OTP);

            boolean result = otpService.verifyOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL, TEST_OTP);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return true when OTP matches in Redis cache")
        void shouldReturnTrueWhenOtpMatchesInRedisCache() {
            String key = "otp:email_verify:" + TEST_EMAIL;
            when(otpCache.getIfPresent(key)).thenReturn(null);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(TEST_OTP);

            boolean result = otpService.verifyOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL, TEST_OTP);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when OTP does not match")
        void shouldReturnFalseWhenOtpDoesNotMatch() {
            String key = "otp:email_verify:" + TEST_EMAIL;
            when(otpCache.getIfPresent(key)).thenReturn("654321");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn("654321");

            boolean result = otpService.verifyOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL, TEST_OTP);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when OTP is not found")
        void shouldReturnFalseWhenOtpNotFound() {
            String key = "otp:email_verify:" + TEST_EMAIL;
            when(otpCache.getIfPresent(key)).thenReturn(null);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(null);

            boolean result = otpService.verifyOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL, TEST_OTP);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should delete OTP from both caches after successful verification from local cache")
        void shouldDeleteOtpAfterSuccessfulVerificationFromLocalCache() {
            String key = "otp:email_verify:" + TEST_EMAIL;
            when(otpCache.getIfPresent(key)).thenReturn(TEST_OTP);
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            otpService.verifyOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL, TEST_OTP);

            verify(otpCache).invalidate(key);
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("Should delete OTP from both caches after successful verification from Redis")
        void shouldDeleteOtpAfterSuccessfulVerificationFromRedis() {
            String key = "otp:email_verify:" + TEST_EMAIL;
            when(otpCache.getIfPresent(key)).thenReturn(null);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(TEST_OTP);

            otpService.verifyOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL, TEST_OTP);

            verify(otpCache).invalidate(key);
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("Should not delete OTP when verification fails")
        void shouldNotDeleteOtpWhenVerificationFails() {
            String key = "otp:email_verify:" + TEST_EMAIL;
            when(otpCache.getIfPresent(key)).thenReturn(null);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(key)).thenReturn(null);

            otpService.verifyOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL, TEST_OTP);

            verify(otpCache, never()).invalidate(any());
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("Should verify OTP case-insensitively")
        void shouldVerifyOtpCaseInsensitively() {
            String key = "otp:email_verify:" + TEST_EMAIL;
            when(otpCache.getIfPresent(key)).thenReturn("ABCdef");
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            boolean result = otpService.verifyOtp(OtpType.EMAIL_VERFICATION, TEST_EMAIL, "abcDEF");

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("OtpType enum tests")
    class OtpTypeTests {

        @Test
        @DisplayName("EMAIL_VERFICATION should have correct prefix")
        void emailVerificationShouldHaveCorrectPrefix() {
            assertEquals(OtpType.EMAIL_VERFICATION.getPrefix(), "email_verify");
            assertEquals(OtpType.EMAIL_VERFICATION.toString(), "email_verify");
        }

        @Test
        @DisplayName("PASSWORD_RECOVERY should have correct prefix")
        void passwordRecoveryShouldHaveCorrectPrefix() {
            assertEquals(OtpType.PASSWORD_RECOVERY.getPrefix(), "reset_pass");
            assertEquals(OtpType.PASSWORD_RECOVERY.toString(), "reset_pass");
        }
    }
}
