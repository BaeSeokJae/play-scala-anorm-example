package controllers

import javax.inject.Inject
import models._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import views._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Manage a database of computers
  */
class HomeController @Inject()(contentService: ContentRepository,
                               memberService: MemberRepository,
                               cc: MessagesControllerComponents)(implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger(this.getClass)

  /**
    * This result directly redirect to the application home.
    */
  val Home = Redirect(routes.HomeController.list(0, 2, ""))

  /**
    * Describe the computer form (used in both edit and create screens).
    */
  val contentForm = Form(
    mapping(
      "id" -> ignored(None: Option[Long]),
      "title" -> nonEmptyText,
      "content" -> nonEmptyText,
      "member" -> optional(longNumber)
    )(Content.apply)(Content.unapply)
  )

  // -- Actions

  /**
    * Handle default path requests, redirect to computers list
    */
  def index = Action {
    Home
  }

  /**
    * Display the paginated list of computers.
    *
    * @param page    Current page number (starts from 0)
    * @param orderBy Column to be sorted
    * @param filter  Filter applied on computer names
    */
  def list(page: Int, orderBy: Int, filter: String) = Action.async { implicit request =>
    contentService.list(page = page, orderBy = orderBy, filter = ("%" + filter + "%")).map { page =>
      Ok(html.list(page[Content, Member], orderBy, filter))
    }
  }

  /**
    * Display the 'edit form' of a existing Computer.
    *
    * @param id Id of the computer to edit
    */
  def edit(id: Long) = Action.async { implicit request =>
    contentService.findById(id).flatMap {
      case Some(content) =>
        memberService.options.map { options =>
          Ok(html.editForm(id, contentForm.fill(content), options))
        }
      case other =>
        Future.successful(NotFound)
    }
  }

  /**
    * Handle the 'edit form' submission
    *
    * @param id Id of the computer to edit
    */
  def update(id: Long) = Action.async { implicit request =>
    contentForm.bindFromRequest().fold(
      formWithErrors => {
        logger.warn(s"form error: $formWithErrors")
        memberService.options.map { options =>
          BadRequest(html.editForm(id, formWithErrors, options))
        }
      },
      content => {
        contentService.update(id, content).map { _ =>
          Home.flashing("success" -> "Content %s has been updated".format(content.title))
        }
      }
    )
  }

  /**
    * Display the 'new computer form'.
    */
  def create = Action.async { implicit request =>
    memberService.options.map { options =>
      Ok(html.createForm(contentForm, options))
    }
  }

  /**
    * Handle the 'new computer form' submission.
    */
  def save = Action.async { implicit request =>
    contentForm.bindFromRequest().fold(
      formWithErrors => memberService.options.map { options =>
        BadRequest(html.createForm(formWithErrors, options))
      },
      content => {
        contentService.insert(content).map { _ =>
          Home.flashing("success" -> "Content %s has been created".format(content.title))
        }
      }
    )
  }

  /**
    * Handle computer deletion.
    */
  def delete(id: Long) = Action.async {
    contentService.delete(id).map { _ =>
      Home.flashing("success" -> "Computer has been deleted")
    }
  }

}
