package ir.tax.trustedtaxpayers.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

	@Bean
	public RedisConnectionFactory redisConnectionFactory(
			@Value("${spring.redis.host:localhost}") String host,
			@Value("${spring.redis.port:6379}") int port,
			@Value("${spring.redis.password:}") String password) {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(host);
		config.setPort(port);
		if (password != null && !password.isBlank()) {
			config.setPassword(password);
		}
		
		// Configure Lettuce to handle connection failures gracefully
		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
				.clientOptions(ClientOptions.builder()
						.socketOptions(SocketOptions.builder()
								.connectTimeout(Duration.ofSeconds(2))
								.build())
						.timeoutOptions(TimeoutOptions.builder()
								.fixedTimeout(Duration.ofSeconds(2))
								.build())
						.autoReconnect(true)
						.build())
				.build();
		
		LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
		// Don't validate connection on startup - allow lazy initialization
		factory.setValidateConnection(false);
		return factory;
	}

	/**
	 * ObjectMapper با default typing فعال برای حفظ نوع کلاس‌ها در serialization
	 * این باعث می‌شود که هنگام deserialization، داده‌ها به LinkedHashMap تبدیل نشوند
	 * و به نوع اصلی (Entity/DTO) برگردند.
	 */
	@Bean
	public ObjectMapper redisObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();

		// Visibility برای همه فیلدها
		mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

		// فعال‌سازی default typing برای حفظ نوع کلاس‌ها
		mapper.activateDefaultTyping(
				LaissezFaireSubTypeValidator.instance,
				ObjectMapper.DefaultTyping.NON_FINAL,
				JsonTypeInfo.As.PROPERTY
		);

		// ⚡ اضافه کردن JavaTimeModule برای پشتیبانی از OffsetDateTime, LocalDateTime و سایر کلاس‌های تاریخ
		mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

		// تاریخ‌ها به صورت timestamp عددی نباشند
		mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		return mapper;
	}


	/**
	 * GenericJackson2JsonRedisSerializer با ObjectMapper دارای default typing
	 * این serializer برای value و hash value استفاده می‌شود
	 */
	@Bean
	public GenericJackson2JsonRedisSerializer redisValueSerializer(ObjectMapper redisObjectMapper) {
		return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
	}

	/**
	 * RedisTemplate با serializer های صحیح
	 * - Key: StringRedisSerializer
	 * - Value: GenericJackson2JsonRedisSerializer با default typing
	 * - Hash Key: StringRedisSerializer
	 * - Hash Value: GenericJackson2JsonRedisSerializer با default typing
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(
			RedisConnectionFactory connectionFactory,
			GenericJackson2JsonRedisSerializer redisValueSerializer) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		
		StringRedisSerializer keySerializer = new StringRedisSerializer();
		
		// تنظیم serializer ها
		template.setKeySerializer(keySerializer);
		template.setHashKeySerializer(keySerializer);
		template.setValueSerializer(redisValueSerializer);
		template.setHashValueSerializer(redisValueSerializer);
		
		// تنظیم default serializer برای عملیات دیگر
		template.setDefaultSerializer(redisValueSerializer);
		
		template.afterPropertiesSet();
		return template;
	}

	/**
	 * CacheManager با Redis و serializer صحیح
	 * این CacheManager برای @Cacheable, @CacheEvict و سایر annotation های cache استفاده می‌شود
	 */
	@Bean
	public CacheManager cacheManager(
			RedisConnectionFactory connectionFactory,
			GenericJackson2JsonRedisSerializer redisValueSerializer) {
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
			.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer))
			.entryTtl(Duration.ofHours(1)); // TTL پیش‌فرض 1 ساعت
		
		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(config)
			.transactionAware()
			.build();
	}
}


