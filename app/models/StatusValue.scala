package models

import play.api.db._
import play.api.Play.current

import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.{ExtendedTable => Table}

import org.scalaquery.ql.extended.H2Driver.Implicit._

import org.scalaquery.session.{Database, Session}
import org.scalaquery.ql.Query
import java.util.Date
import models.DateMapper.date2timestamp
import play.api.libs.json.{Json, JsValue}
import statusValues.{MetricStatus, BuildStatus, HostsStatus}
import globals.Global

case class StatusValue(id: Option[Long],
                       statusMonitorId: Long,
                       statusNum: Int,
                       retrievedAt: Date,
                       valuesJson: String) {

  def this(statusMonitorId: Long, status: StatusTypes.Type, json: JsValue) =
    this(None, statusMonitorId, status.id, new Date(), Json.stringify(json))

  def status = StatusTypes(statusNum)

  def statusValues = Json.parse(valuesJson)

  def buildStatus = {
    if (status != StatusTypes.Unknown) {
      Some(Json.fromJson[BuildStatus](statusValues))
    } else {
      None
    }
  }

  def hostsStatus = {
    if (status != StatusTypes.Unknown) {
      Some(Json.fromJson[HostsStatus](statusValues))
    } else {
      None
    }
  }

  def metricStatus = {
    if (status != StatusTypes.Unknown) {
      Some(Json.fromJson[MetricStatus](statusValues))
    } else {
      None
    }
  }

  def insert = {
    StatusValue.database.withSession {
      implicit db: Session =>
        StatusValue.insert(this)
    }
    Global.displayUpdater ! this
  }

  def update = {
    StatusValue.database.withSession {
      implicit db: Session =>
        StatusValue.where(_.id === id).update(this)
    }
    Global.displayUpdater ! this
  }

  def delete = {
    StatusValue.database.withSession {
      implicit db: Session =>
        StatusValue.where(_.id === id).delete
    }
    Global.displayUpdater ! this
  }
}

object StatusValue extends Table[StatusValue]("STATUSVALUE") {
  def database = Database.forDataSource(DB.getDataSource())

  def id = column[Long]("ID", O PrimaryKey, O AutoInc)

  def statusMonitorId = column[Long]("STATUSMONITORID", O NotNull)

  def statusNum = column[Int]("STATUSNUM", O NotNull)

  def retrievedAt = column[Date]("RETRIEVEDAT", O NotNull)

  def valuesJson = column[String]("VALUESJSON", O NotNull)

  def * = id.? ~ statusMonitorId ~ statusNum ~ retrievedAt ~ valuesJson <>((apply _).tupled, unapply _)

  def query = Query(this)

  def findAllForStatusMonitor(statusMonitorId: Long): Seq[StatusValue] = database.withSession {
    implicit db: Session =>
      query.where(s => s.statusMonitorId === statusMonitorId).orderBy(id.desc).list
  }

  def findLastForStatusMonitor(statusMonitorId: Long): Option[StatusValue] = database.withSession {
    implicit db: Session =>
      query.where(s => s.statusMonitorId === statusMonitorId).orderBy(id.desc).firstOption
  }
}