package com.fintech.ewallet.device.infrastructure.security;

import com.fintech.ewallet.device.domain.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

/**
 * Redis-based OTP service implementation.
 * <p>
 * Phase 1.4: Logs OTP to console instead of sending SMS.
 * Phase 2+: Integrate Twilio/AWS SNS for real SMS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisOtpService implements OtpService {

    private static final String OTP_PREFIX = "otp:code:";
    private static final String RATE_LIMIT_PREFIX = "otp:ratelimit:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT_TTL = Duration.ofHours(1);
    private static final int MAX_OTP_REQUESTS_PER_HOUR = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final Random random = new Random();

    @Override
    public String generateAndSendOtp(String phoneNumber) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", random.nextInt(1000000));

        // Store in Redis with TTL
        String key = OTP_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(key, otp, OTP_TTL);

        // Increment rate limit counter
        String rateLimitKey = RATE_LIMIT_PREFIX + phoneNumber;
        redisTemplate.opsForValue().increment(rateLimitKey);
        redisTemplate.expire(rateLimitKey, RATE_LIMIT_TTL);

        // TODO Phase 2: Send via Twilio/AWS SNS
        log.info("📱 OTP for {}: {} (expires in 5 minutes)", phoneNumber, otp);

        return otp;
    }

    @Override
    public boolean verifyOtp(String phoneNumber, String otp) {
        String key = OTP_PREFIX + phoneNumber;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            // OTP is valid — delete it so it can't be reused
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }

    @Override
    public boolean isRateLimitExceeded(String phoneNumber) {
        String key = RATE_LIMIT_PREFIX + phoneNumber;
        String count = redisTemplate.opsForValue().get(key);

        if (count == null) {
            return false;
        }

        return Integer.parseInt(count) >= MAX_OTP_REQUESTS_PER_HOUR;
    }
}
