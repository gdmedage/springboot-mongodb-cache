/**
 * 
 */
package com.springboot.mongodb.cache.config;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.springboot.mongodb.cache.mongo.CustomKeyGenerator;
import com.springboot.mongodb.cache.mongo.MongoCacheBuilder;
import com.springboot.mongodb.cache.mongo.MongoCacheManager;

/**
 * @author Ganesh.medage
 * @Email gdmedage@gmail.com
 */
@Configuration
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnMissingBean(CacheManager.class)
@EnableConfigurationProperties(MongoCacheProperties.class)
public class MongoCacheAutoConfiguration extends CachingConfigurerSupport {
	
	@Bean
    @ConditionalOnProperty(prefix = "spring.mongo.cache", name = {"collectionName", "cacheName"})
	public CacheManager cacheManager(MongoTemplate mongoTemplate, MongoCacheProperties mongoCacheProperties) {
		MongoCacheBuilder cache = MongoCacheBuilder.newInstance(mongoCacheProperties.getCollectionName(), mongoTemplate, mongoCacheProperties.getCacheName());
		cache.withTTL(mongoCacheProperties.getTtl());
		Collection<MongoCacheBuilder> caches = new ArrayList<>();
		caches.add(cache);
		return new MongoCacheManager(caches);
		
	}
	
	@Bean("customKeyGenerator")
	@Override
	public KeyGenerator keyGenerator() {
		return new CustomKeyGenerator();
	}
}
