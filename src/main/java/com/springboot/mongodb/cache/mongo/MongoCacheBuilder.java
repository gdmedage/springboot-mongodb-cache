/**
 * 
 */
package com.springboot.mongodb.cache.mongo;

import java.util.concurrent.TimeUnit;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.Assert;

/**
 * @author Ganesh.medage
 * @Email gdmedage@gmail.com
 */
public class MongoCacheBuilder {
	private static final long DEFAULT_TTL = TimeUnit.DAYS.toSeconds(7);

    private boolean flushOnBoot;
    private String cacheName;
    private String collectionName;
    private MongoTemplate mongoTemplate;
    private long ttl;

    /**
     * Constructor.
     *
     * @param collectionName a collection name.
     * @param mongoTemplate  a {@link MongoTemplate} instance.
     * @param cacheName      a name of the cache.
     */
    protected MongoCacheBuilder(final String collectionName, final MongoTemplate mongoTemplate, final String cacheName) {
        Assert.hasText(collectionName, "'collectionName' must be not null and must contain at least one non-whitespace character.");
        Assert.notNull(mongoTemplate, "'mongoTemplate' must be not null.");
        Assert.hasText(cacheName, "'cacheName' must be not null and must contain at least one non-whitespace character.");

        this.cacheName = cacheName;
        this.collectionName = collectionName;
        this.mongoTemplate = mongoTemplate;
        this.ttl = DEFAULT_TTL;
    }

    /**
     * Create a new builder instance with the given collection name.
     *
     * @param collectionName a collection name.
     * @param mongoTemplate  a {@link MongoTemplate} instance.
     * @param cacheName      a name of the cache.
     * @return a new builder
     */
    public static MongoCacheBuilder newInstance(String collectionName, MongoTemplate mongoTemplate, String cacheName) {
        return new MongoCacheBuilder(collectionName, mongoTemplate, cacheName);
    }

    /**
     * Build a new {@link MongoCache} with the specified name.
     *
     * @return a {@link MongoCache} instance.
     */
    public MongoCache build() {
        return new MongoCache(cacheName, collectionName, mongoTemplate, ttl, flushOnBoot);
    }

    /**
     * Give a value that indicates if the collection must be always flush.
     *
     * @param flushOnBoot a value that indicates if the collection must be always flush.
     * @return this builder for chaining.
     */
    public MongoCacheBuilder withFlushOnBoot(boolean flushOnBoot) {
        this.flushOnBoot = flushOnBoot;
        return this;
    }

    /**
     * Give a TTL to the cache to be built.
     *
     * @param ttl a time-to-live (in seconds).
     * @return this builder for chaining.
     */
    public MongoCacheBuilder withTTL(long ttl) {
        this.ttl = ttl;
        return this;
    }

}
