package controllers

import play.api.mvc.{Action, Controller}
import models._
import play.api.data.Form
import play.api.data.Forms._
import utils.{DisplayUpdate, DataDigest}
import widgets.Widget
import globals.Global
import actors.DisplayUpdater
import play.api.libs.json.Json
import concurrent.Promise
import play.libs.Akka

object Displays extends Controller {
  implicit val executor = Akka.system.dispatcher

  def index = Action {
    Ok(views.html.display.index(Display.findAll, Sprint.findAll, Project.findAll, displayForm()))
  }

  def create = Action {
    implicit request =>
      displayForm().bindFromRequest().fold(
      formWithErrors => BadRequest(views.html.display.index(Display.findAll, Sprint.findAll, Project.findAll, formWithErrors)), {
        display =>
          display.insert
          Ok(views.html.display.index(Display.findAll, Sprint.findAll, Project.findAll, displayForm()))
      })
  }

  def showConfig(displayId: Long) = Action {
    Display.findById(displayId).map {
      display =>
        Ok(views.html.display.showConfig(display,
          Sprint.findAll,
          Project.findAll,
          displayForm(display),
          DisplayItem.findAllForDisplay(displayId),
          DisplayItems.displayItemFrom(display)))
    }.getOrElse(NotFound)
  }

  def showWall(displayId: Long) = Action {
    implicit request =>
      Display.findById(displayId).map {
        display =>
          val displayItems = DisplayItem.findAllForDisplay(displayId)
          if (display.useLongPolling) {
            val renderedWidgets = displayItems.map {
              displayItem =>
                Widget.forDisplayItem(displayItem).render(display, displayItem)
            }
            Ok(views.html.display.showWall(display, renderedWidgets))
          } else {
            val etag = getEtag(display, displayItems)

            request.headers.get(IF_NONE_MATCH).filter(_ == etag).map(_ => NotModified).getOrElse {
              val renderedWidgets = displayItems.map {
                displayItem =>
                  Widget.forDisplayItem(displayItem).render(display, displayItem)
              }
              Ok(views.html.display.showWall(display, renderedWidgets)).withHeaders(ETAG -> etag)
            }
          }
      }.getOrElse(NotFound)
  }

  def wallUpdates(displayId: Long) = Action(parse.json) {
    implicit request =>
      Display.findById(displayId).map {
        display =>
          Async {
            val result = Promise[DisplayUpdate]()

            Global.displayUpdater ! DisplayUpdater.FindUpdates(display, request.body.as[Map[String, String]], result)
            result.future.map {
              displayUpdate =>
                Ok(Json.stringify(Json.toJson(displayUpdate)))
            }
          }
      }.getOrElse(NotFound)
  }

  def update(displayId: Long) = Action {
    implicit request =>
      Display.findById(displayId).map {
        display =>
          displayForm(display).bindFromRequest.fold(
          formWithErrors => BadRequest(views.html.display.showConfig(display,
            Sprint.findAll,
            Project.findAll,
            formWithErrors,
            DisplayItem.findAllForDisplay(displayId),
            DisplayItems.displayItemFrom(display))), {
            display =>
              display.update
              Redirect(routes.Displays.showConfig(displayId))
          })
      }.getOrElse(NotFound)
  }

  def delete(displayId: Long) = Action {
    implicit request =>
      Display.findById(displayId).map {
        display =>
          display.delete
          Ok(views.html.display.list(Display.findAll))
      }.getOrElse(NotFound)
  }

  private def getEtag(display: Display, displayItems: Seq[DisplayItem]): String = {
    val dataDigest = DataDigest()

    dataDigest.update(display.id)
    dataDigest.update(display.sprintId)
    dataDigest.update(display.backgroundColor)
    dataDigest.update(display.refreshTime)
    displayItems.foreach {
      displayItem =>
        dataDigest.update(Widget.getRenderedWidget(display, displayItem).etag)
    }
    dataDigest.base64Digest()
  }

  private def displayForm(display: Display = new Display): Form[Display] = Form(
    mapping(
      "id" -> ignored(display.id),
      "name" -> text(maxLength = 255),
      "sprintId" -> longNumber,
      "projectId" -> optional(longNumber),
      "backgroundColor" -> text,
      "refreshTime" -> number(min = 1, max = 3600),
      "useLongPolling" -> boolean
    )(Display.apply)(Display.unapply)).fill(display)
}
