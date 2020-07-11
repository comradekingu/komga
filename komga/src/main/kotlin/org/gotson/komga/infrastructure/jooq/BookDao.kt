package org.gotson.komga.infrastructure.jooq

import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.BookSearch
import org.gotson.komga.domain.persistence.BookRepository
import org.gotson.komga.jooq.Tables
import org.gotson.komga.jooq.tables.records.BookRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class BookDao(
  private val dsl: DSLContext
) : BookRepository {

  private val b = Tables.BOOK
  private val m = Tables.MEDIA
  private val d = Tables.BOOK_METADATA

  override fun findByIdOrNull(bookId: Long): Book? =
    findByIdOrNull(dsl, bookId)

  private fun findByIdOrNull(dsl: DSLContext, bookId: Long): Book? =
    dsl.selectFrom(b)
      .where(b.ID.eq(bookId))
      .fetchOneInto(b)
      ?.toDomain()

  override fun findBySeriesId(seriesId: Long): Collection<Book> =
    dsl.selectFrom(b)
      .where(b.SERIES_ID.eq(seriesId))
      .fetchInto(b)
      .map { it.toDomain() }

  override fun findAll(): Collection<Book> =
    dsl.select(*b.fields())
      .from(b)
      .fetchInto(b)
      .map { it.toDomain() }

  override fun findAll(bookSearch: BookSearch): Collection<Book> =
    dsl.select(*b.fields())
      .from(b)
      .leftJoin(d).on(b.ID.eq(d.BOOK_ID))
      .leftJoin(m).on(b.ID.eq(m.BOOK_ID))
      .where(bookSearch.toCondition())
      .fetchInto(b)
      .map { it.toDomain() }


  override fun getLibraryId(bookId: Long): Long? =
    dsl.select(b.LIBRARY_ID)
      .from(b)
      .where(b.ID.eq(bookId))
      .fetchOne(0, Long::class.java)

  override fun findFirstIdInSeries(seriesId: Long): Long? =
    dsl.select(b.ID)
      .from(b)
      .leftJoin(d).on(b.ID.eq(d.BOOK_ID))
      .where(b.SERIES_ID.eq(seriesId))
      .orderBy(d.NUMBER_SORT)
      .limit(1)
      .fetchOne(0, Long::class.java)

  override fun findAllIdBySeriesId(seriesId: Long): Collection<Long> =
    dsl.select(b.ID)
      .from(b)
      .where(b.SERIES_ID.eq(seriesId))
      .fetch(0, Long::class.java)

  override fun findAllIdBySeriesIds(seriesIds: Collection<Long>): Collection<Long> =
    dsl.select(b.ID)
      .from(b)
      .where(b.SERIES_ID.`in`(seriesIds))
      .fetch(0, Long::class.java)

  override fun findAllIdByLibraryId(libraryId: Long): Collection<Long> =
    dsl.select(b.ID)
      .from(b)
      .where(b.LIBRARY_ID.eq(libraryId))
      .fetch(0, Long::class.java)

  override fun findAllId(bookSearch: BookSearch): Collection<Long> {
    val conditions = bookSearch.toCondition()

    return dsl.select(b.ID)
      .from(b)
      .leftJoin(m).on(b.ID.eq(m.BOOK_ID))
      .leftJoin(d).on(b.ID.eq(d.BOOK_ID))
      .where(conditions)
      .fetch(0, Long::class.java)
  }


  override fun insert(book: Book): Book =
    insert(dsl, book)

  override fun insertMany(books: Collection<Book>): Collection<Book> =
    dsl.transactionResult { config ->
      books.map { insert(config.dsl(), it) }
    }

  private fun insert(dsl: DSLContext, book: Book): Book {
    val record = dsl.insertInto(b)
      .set(b.NAME, book.name)
      .set(b.URL, book.url.toString())
      .set(b.NUMBER, book.number)
      .set(b.FILE_LAST_MODIFIED, book.fileLastModified)
      .set(b.FILE_SIZE, book.fileSize)
      .set(b.LIBRARY_ID, book.libraryId)
      .set(b.SERIES_ID, book.seriesId)
      .returning(b.ID)
      .fetchOne()

    return findByIdOrNull(dsl, record.id)!!
  }

  override fun update(book: Book) {
    update(dsl, book)
  }

  override fun updateMany(books: Collection<Book>) {
    dsl.transaction { config ->
      books.map { update(config.dsl(), it) }
    }
  }

  private fun update(dsl: DSLContext, book: Book) {
    dsl.update(b)
      .set(b.NAME, book.name)
      .set(b.URL, book.url.toString())
      .set(b.NUMBER, book.number)
      .set(b.FILE_LAST_MODIFIED, book.fileLastModified)
      .set(b.FILE_SIZE, book.fileSize)
      .set(b.LIBRARY_ID, book.libraryId)
      .set(b.SERIES_ID, book.seriesId)
      .set(b.LAST_MODIFIED_DATE, LocalDateTime.now(ZoneId.of("Z")))
      .where(b.ID.eq(book.id))
      .execute()
  }

  override fun delete(bookId: Long) {
    dsl.transaction { config ->
      with(config.dsl()) {
        deleteFrom(b).where(b.ID.eq(bookId)).execute()
      }
    }
  }

  override fun deleteByBookIds(bookIds: Collection<Long>) {
    dsl.transaction { config ->
      with(config.dsl()) {
        deleteFrom(b).where(b.ID.`in`(bookIds)).execute()
      }
    }
  }

  override fun deleteAll() {
    dsl.transaction { config ->
      with(config.dsl()) {
        deleteFrom(b).execute()
      }
    }
  }

  override fun count(): Long = dsl.fetchCount(b).toLong()


  private fun BookSearch.toCondition(): Condition {
    var c: Condition = DSL.trueCondition()

    libraryIds?.let { c = c.and(b.LIBRARY_ID.`in`(it)) }
    seriesIds?.let { c = c.and(b.SERIES_ID.`in`(it)) }
    searchTerm?.let { c = c.and(d.TITLE.containsIgnoreCase(it)) }
    mediaStatus?.let { c = c.and(m.STATUS.`in`(it)) }

    return c
  }

  private fun BookRecord.toDomain() =
    Book(
      name = name,
      url = URL(url),
      fileLastModified = fileLastModified,
      fileSize = fileSize,
      id = id,
      libraryId = libraryId,
      seriesId = seriesId,
      createdDate = createdDate.toCurrentTimeZone(),
      lastModifiedDate = lastModifiedDate.toCurrentTimeZone(),
      number = number
    )
}
