# com.utils

`com.utils` contains generic utility code shared by multiple modules. The current implementation provides `CacheService` and `AbstractCacheService` as generic cache contracts, with the concrete `JavaCacheService` implementation in `com.utils.cache.jdk`.

Configuration loading reads `CacheConfigurationService` from each module's cache configuration and creates the cache through `CacheServiceFactory`. The current provider values are `java` for the in-memory Java cache and `none` to disable caching without changing consuming code.
