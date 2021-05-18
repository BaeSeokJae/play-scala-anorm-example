package models

import java.util.Date
import javax.inject.Inject
import anorm.SqlParser.{get, scalar}
import anorm._
import play.api.db.DBApi

import scala.concurrent.Future

case class Content(id: Option[Long] = None,
                   title: String,
                   content: String,
                   memberId: Option[Long])

object Content {
    implicit def toParameters: ToParameterList[Content] =
        Macro.toParameters[Content]
}

/**
 * Helper for pagination.
 */
case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
    lazy val prev = Option(page - 1).filter(_ >= 0)
    lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}


@javax.inject.Singleton
class ContentRepository @Inject()(dbapi: DBApi, memberRepository: MemberRepository)(implicit ec: DatabaseExecutionContext) {

    private val db = dbapi.database("default")

    // -- Parsers

    /**
     * Parse a Content from a ResultSet
     */
    private val simple = {
        get[Option[Long]]("content.id") ~
          get[String]("content.title") ~
          get[String]("content.content") ~
          get[Option[Long]]("content.member_id") map {
            case id ~ title ~ content ~ memberId =>
                Content(id, title, content, memberId)
        }
    }

    private val withMember = simple ~ (memberRepository.simple.?) map {
        case content ~ member => content -> member
    }

    // -- Queries

    def findById(id: Long): Future[Option[Content]] = Future {
        db.withConnection { implicit connection =>
            SQL"select * from content where id = $id".as(simple.singleOpt)
        }
    }(ec)

    /**
     * Return a page of (Content,Member).
     *
     * @param page     Page to display
     * @param pageSize Number of contents per page
     * @param orderBy  Content property used for sorting
     * @param filter   Filter applied on the name column
     */
    def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Future[Page[(Content, Option[Member])]] = Future {

        val offset = pageSize * page

        db.withConnection { implicit connection =>

            val contents =
                SQL"""
        select * from content
        left join member on content.member_id = member.id
        where content.title like ${filter}
        order by ${orderBy} nulls last
        limit ${pageSize} offset ${offset}
      """.as(withMember.*)

            val totalRows =
                SQL"""
        select count(*) from content
        left join member on content.member_id = member.id
        where content.title like ${filter}
      """.as(scalar[Long].single)

            Page(contents, page, offset, totalRows)
        }
    }(ec)

    /**
     * Update a content.
     *
     * @param id      The content id
     * @param content The content values.
     */
    def update(id: Long, content: Content) = Future {
        db.withConnection { implicit connection =>
            SQL(
                """
        update content set title = {title}, content = {content}, member_id = {memberId}
        where id = {id}
      """).bind(content.copy(id = Some(id) /* ensure */)).executeUpdate()
            // case class binding using ToParameterList,
            // note using SQL(..) but not SQL.. interpolation
        }
    }(ec)

    /**
     * Insert a new content.
     *
     * @param content The content values.
     */
    def insert(content: Content): Future[Option[Long]] = Future {
        db.withConnection { implicit connection =>
            SQL(
                """
        insert into content values (
          (select next value for content_seq),
          {title}, {content}, {memberId}
          )
      """).bind(content).executeInsert()
        }
    }(ec)

    /**
     * Delete a content.
     *
     * @param id Id of the content to delete.
     */
    def delete(id: Long) = Future {
        db.withConnection { implicit connection =>
            SQL"delete from content where id = ${id}".executeUpdate()
        }
    }(ec)

}
