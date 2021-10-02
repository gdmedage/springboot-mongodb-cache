/**
 * 
 */
package com.springboot.mongodb.cache.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * @author Ganesh.medage
 * @Email gdmedage@gmail.com
 */
@Configuration
@ConditionalOnMissingBean(MongoTemplate.class)
@EnableConfigurationProperties(MongoDBProperties.class)
public class MongoAutoConfiguration {
	
	@Bean
    @ConditionalOnProperty(prefix = "spring.data.mongodb", name = {"host", "port", "userName", "password", "database", "autSource", "authMechanism", "ssl"})
	public MongoTemplate mongoTemplate(MongoDBProperties mongoDBProperties) {
		final String uri = "mongodb://" + mongoDBProperties.getUserName() + ":" + mongoDBProperties.getPassword() + "@" + mongoDBProperties.getHost() + ":" + mongoDBProperties.getPort() + "/" + mongoDBProperties.getDatabase() + "?authMechanism=" + mongoDBProperties.getAuthMechanism() + "&authSource=" + mongoDBProperties.getAuthSource() + "&ssl=" + mongoDBProperties.getSsl();
		MongoClient mongoClient = MongoClients.create(uri);
		return new MongoTemplate(mongoClient, mongoDBProperties.getDatabase());
	}

}
