package models

object DisplayWidgets extends Enumeration {
  type Type = Value
  val BurndownChart, SprintTitle, Clock, Alarms, IFrame, BuildStatus, HostStatus, Metrics = Value
}
