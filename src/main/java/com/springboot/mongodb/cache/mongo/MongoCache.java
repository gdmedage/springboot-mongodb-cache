/**
 * 
 */
package com.springboot.mongodb.cache.mongo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;

import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;
import com.springboot.mongodb.cache.model.CacheDocument;

/**
 * @author Ganesh.medage
 * @Email gdmedage@gmail.com
 */
public class MongoCache implements Cache{

	private static final long DEFAULT_TTL = TimeUnit.DAYS.toSeconds(30);
    private static final String INDEX_KEY_NAME = "creationDate";
    private static final String INDEX_NAME = "_expire";
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoCache.class);

    private final boolean flushOnBoot;
    private final String collectionName;
    private final String cacheName;
    private final MongoTemplate mongoTemplate;
    private final long ttl;

    /**
     * Constructor.
     *
     * @param cacheName      a cache name.
     * @param collectionName a collection name.
     * @param mongoTemplate  a {@link MongoTemplate} instance.
     */
    public MongoCache(String cacheName, String collectionName, MongoTemplate mongoTemplate) {
        this(cacheName, collectionName, mongoTemplate, DEFAULT_TTL);
    }

    /**
     * Constructor.
     *
     * @param cacheName      a cache name.
     * @param collectionName a collection name.
     * @param mongoTemplate  a {@link MongoTemplate} instance.
     * @param ttl            a time-to-live (in seconds).
     */
    public MongoCache(String cacheName, String collectionName, MongoTemplate mongoTemplate, long ttl) {
        this(cacheName, collectionName, mongoTemplate, ttl, false);
    }

    /**
     * Constructor.
     *
     * @param cacheName      a cache name.
     * @param collectionName a collection name.
     * @param mongoTemplate  a {@link MongoTemplate} instance.
     * @param ttl            a time-to-live (in seconds).
     * @param flushOnBoot    a value that indicates if the collection must be always flush.
     */
    public MongoCache(String cacheName, String collectionName, MongoTemplate mongoTemplate, long ttl, boolean flushOnBoot) {
        Assert.hasText(cacheName, "'cacheName' must be not null and not empty.");
        Assert.notNull(collectionName, "'collectionName' must be not null.");
        Assert.notNull(collectionName, "'mongoTemplate' must be not null.");

        this.flushOnBoot = flushOnBoot;
        this.collectionName = collectionName;
        this.mongoTemplate = mongoTemplate;
        this.cacheName = cacheName;
        this.ttl = ttl;

        initialize();
    }

    private void creationCollection() {
        mongoTemplate.getCollection(collectionName);
    }

    @Override
    public void clear() {
        mongoTemplate.remove(new Query(), CacheDocument.class, collectionName);
    }

    @Override
    public void evict(Object key) {
        Assert.isTrue(key instanceof String, "'key' must be an instance of 'java.lang.String'.");

        final String id = (String) key;
        final CriteriaDefinition criteria = Criteria.where("_id").is(id);
        final Query query = Query.query(criteria);

        mongoTemplate.remove(query, collectionName);
    }

    @Override
    public ValueWrapper get(Object key) {
        final Object value = getFromCache(key);
        if (value != null) {
            return new SimpleValueWrapper(value);
        }

        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        try {
            final Object value = getFromCache(key);
            if (value == null) {
                return null;
            }
            return type.cast(value);
        } catch (ClassCastException e) {
            throw new IllegalStateException("Unable to cast the object.", e);
        }
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Assert.isTrue(key instanceof String, "'key' must be an instance of 'java.lang.String'.");
        Assert.notNull(valueLoader, "'valueLoader' must not be null");

        Object cached = getFromCache(key);
        if (cached != null) {
            return (T) cached;
        }

        final Object dynamicLock = ((String) key).intern();

        synchronized (dynamicLock) {
            cached = getFromCache(key);
            if (cached != null) {
                return (T) cached;
            }

            T value;
            try {
                value = valueLoader.call();
            } catch (Throwable ex) {
                throw new ValueRetrievalException(key, valueLoader, ex);
            }

            ValueWrapper newCachedValue = putIfAbsent(key, value);
            if (newCachedValue != null) {
                return (T) newCachedValue.get();
            } else {
                return value;
            }
        }

    }

    /**
     * Gets whether the cache should delete all elements on boot.
     *
     * @return returns whether the cache should delete all elements on boot.
     */
    public final boolean isFlushOnBoot() {
        return flushOnBoot;
    }

    public String getCollectionName() {
        return collectionName;
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public Object getNativeCache() {
        return mongoTemplate;
    }

    /**
     * Returns the TTL value for this cache.
     *
     * @return the ttl value.
     */
    public final long getTtl() {
        return ttl;
    }

    @Override
    public void put(Object key, Object value) {
        Assert.isTrue(key instanceof String, "'key' must be an instance of 'java.lang.String'.");

        try {
            final String id = (String) key;
            String result = null;
            if (value != null) {
                Assert.isTrue(value instanceof Serializable, "'value' must be serializable.");
                result = serialize(value);
            }

            final CacheDocument cache = new CacheDocument(id, result);
            mongoTemplate.save(cache, collectionName);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Can not serialize the value: %s", key), e);
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        Assert.isTrue(key instanceof String, "'key' must be an instance of 'java.lang.String'.");

        try {
            final String id = (String) key;
            String result = null;

            if (value != null) {
                Assert.isTrue(value instanceof Serializable, "'value' must be serializable.");
                result = serialize(value);
            }

            final CacheDocument cache = new CacheDocument(id, result);
            mongoTemplate.insert(cache, collectionName);
            return null;
        } catch (DuplicateKeyException e) {
            LOGGER.info(String.format("Key: %s already exists in the cache. Element will not be replaced.", key), e);
            return get(key);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Cannot serialize the value for key: %s", key), e);
        }
    }

    private Object deserialize(String value) throws IOException, ClassNotFoundException {
        final Base64.Decoder decoder = Base64.getDecoder();
        final byte[] data = decoder.decode(value);
        try (final ByteArrayInputStream buffer = new ByteArrayInputStream(data);
                final ObjectInputStream output = new ObjectInputStream(buffer)) {
            return output.readObject();
        }
    }

    private Object getFromCache(Object key) {
        Assert.isTrue(key instanceof String, "'key' must be an instance of 'java.lang.String'.");

        final String id = (String) key;
        final CacheDocument cache = mongoTemplate.findById(id, CacheDocument.class, collectionName);

        if (cache != null) {
            final String element = cache.getElement();
            if (element != null && !"".equals(element)) {
                try {
                    return deserialize(element);
                } catch (IOException | ClassNotFoundException e) {
                    throw new IllegalStateException("Unable to read the object from cache.", e);
                }
            }
        }

        return null;
    }

    private void initialize() {
        creationCollection();

        if (isFlushOnBoot()) {
            clear();
        }

        final Index expireIndex = createExpireIndex();
        updateExpireIndex(expireIndex);
    }

    private String serialize(Object value) throws IOException {
        try (final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                final ObjectOutputStream output = new ObjectOutputStream(buffer)) {

            output.writeObject(value);

            final byte[] data = buffer.toByteArray();

            final Base64.Encoder encoder = Base64.getEncoder();
            return encoder.encodeToString(data);
        }
    }

    private Index createExpireIndex() {
        final Index index = new Index();
        index.named(INDEX_NAME);
        index.on(INDEX_KEY_NAME, Sort.Direction.ASC);
        index.expire(ttl);

        return index;
    }

    private void updateExpireIndex(Index newExpireIndex) {
        final IndexOperations indexOperations = mongoTemplate.indexOps(collectionName);
        final MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);
		
        Iterable<Document> iterDoc = collection.find();
        Iterator it = iterDoc.iterator();
        DBObject indexes = null;
        while (it.hasNext()) {
        	indexes = (DBObject) it.next();
        	if(INDEX_NAME.equals(indexes.get("name"))) {
        		break;
        	}
           System.out.println(indexes);
        }
			 
		 if (indexes != null) { 
			 final long ttl1 = (long) indexes.get("expireAfterSeconds");
		 
		 if (ttl1 != this.ttl) { indexOperations.dropIndex(INDEX_NAME); } }
		 
		 indexOperations.ensureIndex(newExpireIndex);
		 
    }
}
