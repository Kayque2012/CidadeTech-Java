package br.com.empauta;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.*;

@Configuration
class CacheConfig {
  @Bean
  CacheManager cacheManager() {
    return new TransactionAwareCacheManagerProxy(
        new ConcurrentMapCacheManager("temas", "resultados"));
  }
}
