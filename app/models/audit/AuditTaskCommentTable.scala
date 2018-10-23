package models.audit

import java.sql.Timestamp

import models.daos.slick.DBTableDefinitions.UserTable
import models.mission.{Mission, MissionTable}
import models.utils.MyPostgresDriver.api._
import play.api.Play.current

import slick.lifted.ForeignKeyQuery

case class AuditTaskComment(auditTaskCommentId: Int, auditTaskId: Int, missionId: Int, edgeId: Int, userId: String,
                            ipAddress: String, gsvPanoramaId: Option[String], heading: Option[Double],
                            pitch: Option[Double],  zoom: Option[Int], lat: Option[Double], lng: Option[Double],
                            timestamp: Timestamp, comment: String)

class AuditTaskCommentTable(tag: Tag) extends Table[AuditTaskComment](tag, Some("sidewalk"), "audit_task_comment") {
  def auditTaskCommentId = column[Int]("audit_task_comment_id", O.PrimaryKey, O.AutoInc)
  def auditTaskId = column[Int]("audit_task_id")
  def missionId = column[Int]("mission_id")
  def edgeId = column[Int]("edge_id")
  def userId = column[String]("user_id")
  def ipAddress = column[String]("ip_address")
  def gsvPanoramaId = column[Option[String]]("gsv_panorama_id")
  def heading = column[Option[Double]]("heading")
  def pitch = column[Option[Double]]("pitch")
  def zoom = column[Option[Int]]("zoom")
  def lat = column[Option[Double]]("lat")
  def lng = column[Option[Double]]("lng")
  def timestamp = column[Timestamp]("timestamp")
  def comment = column[String]("comment")

  def * = (auditTaskCommentId, auditTaskId, missionId, edgeId, userId, ipAddress, gsvPanoramaId, heading, pitch, zoom, lat, lng, timestamp, comment) <>
    ((AuditTaskComment.apply _).tupled, AuditTaskComment.unapply)

  def auditTask: ForeignKeyQuery[AuditTaskTable, AuditTask] =
    foreignKey("audit_task_comment_audit_task_id_fkey", auditTaskId, TableQuery[AuditTaskTable])(_.auditTaskId)

  def mission: ForeignKeyQuery[MissionTable, Mission] =
    foreignKey("audit_task_comment_mission_id_fkey", missionId, TableQuery[MissionTable])(_.missionId)
}

object AuditTaskCommentTable {
  val db = play.api.db.slick.DB
  val auditTaskComments = TableQuery[AuditTaskCommentTable]
  val users = TableQuery[UserTable]

  /**
    * Get all task records of the given user
    * @param username Username
    * @return
    */
  def all(username: String): Option[List[AuditTaskComment]] = db.withTransaction { implicit session =>
    val comments = (for {
      (c, u) <- auditTaskComments.join(users).on(_.userId === _.userId).sortBy(_._1.timestamp.desc) if u.username === username
    } yield (c.auditTaskCommentId, c.auditTaskId, c.missionId, c.edgeId, u.username, c.ipAddress, c.gsvPanoramaId,
      c.heading, c.pitch, c.zoom, c.lat, c.lng, c.timestamp, c.comment)).list.map { c => AuditTaskComment.tupled(c) }

    Some(comments)
  }

  /**
    * Insert an audit_task_comment record.
    *
    * @param comment AuditTaskComment object
    * @return
    */
  def save(comment: AuditTaskComment): Int = db.withTransaction { implicit session =>
    val auditTaskCommentId: Int =
      (auditTaskComments returning auditTaskComments.map(_.auditTaskCommentId)) += comment
    auditTaskCommentId
  }

  /**
    * Take the last n comments.
    *
    * @param n
    * @return
    */
  def takeRight(n: Integer): List[AuditTaskComment] = db.withTransaction { implicit session =>
    val comments = (for {
      (c, u) <- auditTaskComments.join(users).on(_.userId === _.userId).sortBy(_._1.timestamp.desc)
    } yield (c.auditTaskCommentId, c.auditTaskId, c.missionId, c.edgeId, u.username, c.ipAddress, c.gsvPanoramaId,
      c.heading, c.pitch, c.zoom, c.lat, c.lng, c.timestamp, c.comment)).list.map { c => AuditTaskComment.tupled(c) }

    comments.take(n)
  }
}
