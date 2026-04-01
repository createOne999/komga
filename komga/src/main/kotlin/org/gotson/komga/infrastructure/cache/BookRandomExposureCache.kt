package org.gotson.komga.infrastructure.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.gotson.komga.domain.model.BookSearch
import org.gotson.komga.domain.model.SearchContext
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class BookRandomExposureCache {
  private val cache =
    Caffeine
      .newBuilder()
      .expireAfterAccess(60, TimeUnit.MINUTES)
      .build<Key, State>()

  fun key(
    userId: String,
    search: BookSearch,
    context: SearchContext,
  ) = Key(
    userId = userId,
    search = search,
    authorizedLibraryIds = context.libraryIds?.sorted(),
    restrictions = context.restrictions.toString(),
  )

  fun <T> withState(
    key: Key,
    block: (State) -> T,
  ): T {
    val state = cache.get(key) { State() } ?: State()
    return synchronized(state) {
      block(state)
    }
  }

  fun clear() {
    cache.invalidateAll()
  }

  data class Key(
    val userId: String,
    val search: BookSearch,
    val authorizedLibraryIds: List<String>?,
    val restrictions: String,
  )

  class State {
    val exposures: MutableMap<String, Int> = mutableMapOf()
    var rerollCount: Long = 0
  }
}
