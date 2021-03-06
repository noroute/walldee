package controllers.widgets

import play.api.Play.current
import models.widgetConfigs.{SprintTitleConfig, BuildStatusConfig}
import play.api.data.Forms._
import models._
import play.api.templates.Html
import utils.{AtomState, DataDigest}
import xml.NodeSeq
import play.api.mvc.RequestHeader
import play.api.cache.Cache
import org.joda.time.format.ISODateTimeFormat

object BuildStatus extends Widget[BuildStatusConfig] {
  val configMapping = mapping(
    "labelFont" -> optional(text),
    "labelSize" -> optional(number)
  )(BuildStatusConfig.apply)(BuildStatusConfig.unapply)

  override def renderHtml(display: Display, displayItem: DisplayItem): Html = {
    val projectIdOpt = displayItem.projectId.map(Some(_)).getOrElse(display.projectId)
    projectIdOpt.map {
      projectId =>
        val statusMonitorsWithStatus = getStatusMonitorsWithStatus(projectId)
        views.html.display.widgets.buildStatus(display, displayItem, projectId, statusMonitorsWithStatus)
    }.getOrElse(Html(""))
  }

  override def renderAtom(display: Display, displayItem: DisplayItem)
                         (implicit request: RequestHeader): (NodeSeq, Long) = {
    val projectIdOpt = displayItem.projectId.map(Some(_)).getOrElse(display.projectId)
    projectIdOpt.map {
      projectId =>
        val failedStatus = getStatusMonitorsWithStatus(projectId).filter(_._2 != StatusTypes.Ok).map(_._1)
        val html = renderHtml(display, displayItem)
        val dateFormat = ISODateTimeFormat.dateTime().withZoneUTC()
        val lastUpdate = atomLastUpdate(display, displayItem, html)

        (<entry>
          <title>
            {if (failedStatus.isEmpty) "All builds green" else "Build is red"}
          </title>
          <id>
            {controllers.routes.DisplayItems.show(display.id.get, displayItem.id.get).absoluteURL()}
          </id>
          <link href={controllers.routes.DisplayItems.show(display.id.get, displayItem.id.get).absoluteURL()}></link>
          <updated>
            {dateFormat.print(lastUpdate)}
          </updated>
          <summary type="html">
            {if (failedStatus.isEmpty)
            "All builds green"
          else
            "Builds in error: <ul>" + failedStatus.map("<li>" + _.name + "</li>").mkString + "</ul>"}
          </summary>
          <content type="html">
            {html}
          </content>
        </entry>, 0L)
    }.getOrElse((NodeSeq.Empty, 0L))
  }

  private def getStatusMonitorsWithStatus(projectId: Long): Seq[(StatusMonitor, StatusTypes.Type)] = {
    val statusMonitors = StatusMonitor.finaAllForProject(projectId,
      Seq(StatusMonitorTypes.Jenkins, StatusMonitorTypes.Teamcity))
    statusMonitors.map {
      statusMonitor =>
        (statusMonitor,
          StatusValue.findLastForStatusMonitor(statusMonitor.id.get).map(_.status).getOrElse(StatusTypes.Unknown))
    }
  }

  private def atomLastUpdate(display: Display, displayItem: DisplayItem, html: Html): Long = {
    val key = "BuildStatus-%d-%d".format(display.id.get, displayItem.id.get)
    val etag = DataDigest.etag(html)
    var state = Cache.getOrElse(key) {
      AtomState(etag, System.currentTimeMillis())
    }
    if (state.etag != etag) {
      state = AtomState(etag, System.currentTimeMillis())
      Cache.set(key, state)
    }
    state.lastUpdate
  }
}
