/**
 * 
 */
package com.springboot.mongodb.cache.mongo;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * @author Ganesh.medage
 * @Email gdmedage@gmail.com
 */
public class CustomKeyGenerator implements KeyGenerator{

	@Override
	public Object generate(Object target, Method method, Object... params) {
		String keyString = StringUtils.arrayToDelimitedString(params, "_");
		return DigestUtils.md5DigestAsHex(keyString.getBytes());
	}
}
