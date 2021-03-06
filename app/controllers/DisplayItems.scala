package controllers

import play.api.mvc.{Action, Controller}
import models.{Project, Display, DisplayItem}
import play.api.data.Form
import play.api.data.Forms._
import widgets.{Widget, Clock, SprintTitle, BurndownChart}
import models.widgetConfigs.SprintTitleConfig

object DisplayItems extends Controller {
  def create(displayId: Long) = Action {
    implicit request =>
      Display.findById(displayId).map {
        display =>
          displayItemFrom(display).bindFromRequest.fold(
          formWithErrors => BadRequest, {
            displayItem =>
              displayItem.insert
              Redirect(routes.Displays.showConfig(displayId))
          })
      }.getOrElse(NotFound)
  }

  def show(displayId: Long, displayItemId: Long) = Action {
    Display.findById(displayId).flatMap {
      display =>
        DisplayItem.findById(displayItemId).map {
          displayItem =>
            def renderedWidget = Widget.forDisplayItem(displayItem).render(display, displayItem)

            Ok(views.html.displayItem.show(display, displayItem, renderedWidget))
        }
    }.getOrElse(NotFound)
  }

  def edit(displayId: Long, displayItemId: Long) = Action {
    Display.findById(displayId).flatMap {
      display =>
        DisplayItem.findById(displayItemId).map {
          displayItem =>
            Ok(views.html.displayItem.edit(display, displayItem, Project.findAll, displayItemForm(displayItem)))
        }
    }.getOrElse(NotFound)
  }

  def update(displayId: Long, displayItemId: Long) = Action {
    implicit request =>
      Display.findById(displayId).flatMap {
        display =>
          DisplayItem.findById(displayItemId).map {
            displayItem =>
              displayItemForm(displayItem).bindFromRequest.fold(
              formWithErrors => BadRequest(views.html.displayItem.edit(display, displayItem, Project.findAll, formWithErrors)), {
                displayItem =>
                  displayItem.update
                  Redirect(routes.Displays.showConfig(displayId))
              })
          }
      }.getOrElse(NotFound)
  }

  def delete(displayId: Long, displayItemId: Long) = Action {
    implicit request =>
      Display.findById(displayId).flatMap {
        display =>
          DisplayItem.findById(displayItemId).map {
            displayItem =>
              displayItem.delete
              Ok(views.html.displayItem.list(display, DisplayItem.findAllForDisplay(displayId)))
          }
      }.getOrElse(NotFound)
  }

  def displayItemFrom(display: Display) =
    displayItemForm(new DisplayItem(None, display.id.get, 0, 0, 0, 0, 0, 0, None, false, "{}"))

  def displayItemForm(displayItem: DisplayItem) = Form(
    mapping(
      "id" -> ignored(displayItem.id),
      "displayId" -> ignored(displayItem.displayId),
      "posx" -> number(min = 0),
      "posy" -> number(min = 0),
      "width" -> number(min = 0),
      "height" -> number(min = 0),
      "style" -> number,
      "widget" -> number,
      "projectId" -> optional(longNumber),
      "appearsInFeed" -> boolean,
      "burndownChartConfig" -> optional(BurndownChart.configMapping),
      "sprintTitleConfig" -> optional(SprintTitle.configMapping),
      "clockConfig" -> optional(Clock.configMapping),
      "alarmsConfig" -> optional(widgets.Alarms.configMapping),
      "iframeConfig" -> optional(widgets.IFrame.configMapping),
      "buildStatusConfig" -> optional(widgets.BuildStatus.configMapping),
      "hostStatusConfig" -> optional(widgets.HostStatus.configMapping),
      "metricsConfig" -> optional(widgets.Metrics.configMapping)
    )(DisplayItem.formApply)(DisplayItem.formUnapply)).fill(displayItem)
}
