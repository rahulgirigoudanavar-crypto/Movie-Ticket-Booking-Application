package com.jsp.book.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jsp.book.dto.UserDto;
import com.jsp.book.entity.BookedTicket;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

	private static final Logger logger = LoggerFactory.getLogger(RedisServiceImpl.class);

	private static final String USER_DTO_KEY = "dto-";
	private static final String OTP_KEY = "otp-";

	private static final Duration USER_DTO_TTL = Duration.ofMinutes(15);
	private static final Duration OTP_TTL = Duration.ofMinutes(2);
	private static final Duration TICKET_TTL = Duration.ofMinutes(15);

	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	@Async
	public void saveUserDto(String email, UserDto userDto) {
		try {
			redisTemplate.opsForValue().set(USER_DTO_KEY + email, userDto, USER_DTO_TTL);
		} catch (RedisConnectionFailureException e) {
			logger.warn("Redis not available, skipping user DTO cache for email: {}", email);
		} catch (Exception e) {
			logger.error("Error saving user DTO to Redis for email: {}", email, e);
		}
	}

	@Override
	@Async
	public void saveOtp(String email, int otp) {
		try {
			redisTemplate.opsForValue().set(OTP_KEY + email, otp, OTP_TTL);
		} catch (RedisConnectionFailureException e) {
			logger.warn("Redis not available, skipping OTP cache for email: {}", email);
		} catch (Exception e) {
			logger.error("Error saving OTP to Redis for email: {}", email, e);
		}
	}

	@Override
	public UserDto getUserDto(String email) {
		try {
			Object value = redisTemplate.opsForValue().get(USER_DTO_KEY + email);
			return (value instanceof UserDto dto) ? dto : null;
		} catch (RedisConnectionFailureException e) {
			logger.warn("Redis not available, returning null for user DTO: {}", email);
			return null;
		} catch (Exception e) {
			logger.error("Error retrieving user DTO from Redis for email: {}", email, e);
			return null;
		}
	}

	@Override
	public int getOtp(String email) {
		try {
			Object value = redisTemplate.opsForValue().get(OTP_KEY + email);
			return (value instanceof Integer otp) ? otp : 0;
		} catch (RedisConnectionFailureException e) {
			logger.warn("Redis not available, returning 0 for OTP: {}", email);
			return 0;
		} catch (Exception e) {
			logger.error("Error retrieving OTP from Redis for email: {}", email, e);
			return 0;
		}
	}

	@Override
	public void saveTicket(String orderId, BookedTicket ticket) {
		redisTemplate.opsForValue().set(orderId, ticket, TICKET_TTL);
	}

	@Override
	public BookedTicket getTicket(String orderId) {
		Object value = redisTemplate.opsForValue().get(orderId);
		return (value instanceof BookedTicket ticket) ? ticket : null;
	}
}
