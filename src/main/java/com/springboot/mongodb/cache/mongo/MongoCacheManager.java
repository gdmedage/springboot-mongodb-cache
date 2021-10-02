/**
 * 
 */
package com.springboot.mongodb.cache.mongo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.util.Assert;

/**
 * @author Ganesh.medage
 * @Email gdmedage@gmail.com
 */
public class MongoCacheManager extends AbstractCacheManager {

    private final Collection<MongoCacheBuilder> initialCaches;

    /**
     * Constructor.
     *
     * @param initialCaches the caches to make available on startup.
     */
    public MongoCacheManager(final Collection<MongoCacheBuilder> initialCaches) {
        Assert.notEmpty(initialCaches, "At least one cache builder must be specified.");
        this.initialCaches = new ArrayList<>(initialCaches);
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        final Collection<Cache> caches = new LinkedHashSet<>(initialCaches.size());
        for (final MongoCacheBuilder cacheBuilder : initialCaches) {
            final MongoCache cache = cacheBuilder.build();
            caches.add(cache);
        }

        return caches;
    }

}

