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
@ConfigurationProperties(prefix = "spring.data.mongodb")
@Getter
@Setter
public class MongoDBProperties {
	private String host;
	private String port;
	private String database;
	private String userName;
	private String password;
	private String authSource;
	private String authMechanism;
	private String ssl;
	private String uri;
}
