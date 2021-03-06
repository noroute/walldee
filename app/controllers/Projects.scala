package controllers

import play.api.mvc.{Action, Controller}
import models.{StatusMonitor, Project, Sprint}
import play.api.data.Form
import play.api.data.Forms._

object Projects extends Controller {
  def index = Action {
    Ok(views.html.projects.index(Project.findAll, projectForm()))
  }

  def create = Action {
    implicit request =>
      projectForm().bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.projects.index(Project.findAll, formWithErrors)), {
        project =>
          project.insert
          Ok(views.html.projects.index(Project.findAll, projectForm()))
      })
  }

  def edit(projectId: Long) = Action {
    Project.findById(projectId).map {
      project =>
        Ok(views.html.projects.edit(project, projectForm(project)))
    }.getOrElse(NotFound)
  }

  def update(projectId: Long) = Action {
    implicit request =>
      Project.findById(projectId).map {
        project =>
          projectForm(project).bindFromRequest.fold(
          formWithErrors => BadRequest(views.html.projects.edit(project, formWithErrors)), {
            statusMonitor =>
              statusMonitor.update
              Ok(views.html.projects.edit(project, projectForm(project)))
          })
      }.getOrElse(NotFound)
  }

  def delete(projectId: Long) = Action {
    Project.findById(projectId).map {
      project =>
        project.delete
        Ok(views.html.projects.list(Project.findAll))
    }.getOrElse(NotFound)
  }

  private def projectForm(project: Project = new Project): Form[Project] = Form(
    mapping(
      "id" -> ignored(project.id),
      "name" -> text(maxLength = 255)
    )(Project.apply)(Project.unapply)).fill(project)
}
