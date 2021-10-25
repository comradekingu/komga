package org.gotson.komga.domain.persistence

import org.gotson.komga.domain.model.ReadList
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ReadListRepository {
  fun findByIdOrNull(readListId: String): ReadList?

  fun findAll(search: String? = null, pageable: Pageable): Page<ReadList>

  /**
   * Find one ReadList by readListId,
   * optionally with only bookIds filtered by the provided filterOnLibraryIds.
   */
  fun findByIdOrNull(readListId: String, filterOnLibraryIds: Collection<String>?): ReadList?

  /**
   * Find all ReadList with at least one Book belonging to the provided belongsToLibraryIds,
   * optionally with only bookIds filtered by the provided filterOnLibraryIds.
   */
  fun findAllByLibraryIds(belongsToLibraryIds: Collection<String>, filterOnLibraryIds: Collection<String>?, search: String? = null, pageable: Pageable): Page<ReadList>

  /**
   * Find all ReadList that contains the provided containsBookId,
   * optionally with only bookIds filtered by the provided filterOnLibraryIds.
   */
  fun findAllContainingBookId(containsBookId: String, filterOnLibraryIds: Collection<String>?): Collection<ReadList>

  fun findAllEmpty(): Collection<ReadList>

  fun findByNameOrNull(name: String): ReadList?

  fun insert(readList: ReadList)
  fun update(readList: ReadList)

  fun removeBookFromAll(bookId: String)
  fun removeBooksFromAll(bookIds: Collection<String>)

  fun delete(readListId: String)
  fun delete(readListIds: Collection<String>)
  fun deleteAll()

  fun existsByName(name: String): Boolean
}
