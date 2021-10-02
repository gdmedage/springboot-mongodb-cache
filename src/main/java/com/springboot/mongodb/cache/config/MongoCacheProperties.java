/**
 * 
 */
package com.springboot.mongodb.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Ganesh.medage
 * @Email gdmedage@gmail.com
 */
@Configuration
@ConfigurationProperties(prefix = "spring.mongo.cache")
@Getter
@Setter
public class MongoCacheProperties {
	private String cacheName;
	private String collectionName;
	private String flushOnBoot;
	private long ttl;

}
