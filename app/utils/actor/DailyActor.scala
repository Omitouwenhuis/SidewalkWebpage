package utils.actor

import java.util.Calendar

import akka.actor.{Actor, Cancellable, Props}
import models.street.StreetEdgePriorityTable

import scala.concurrent.duration._

// Template code comes from this helpful StackOverflow post:
// https://stackoverflow.com/questions/48977612/how-to-schedule-complex-tasks-using-scala-play-2-3/48977937?noredirect=1#comment84961371_48977937
class DailyActor extends Actor {

  private var cancellable: Option[Cancellable] = None


  override def preStart(): Unit = {
    super.preStart()
    // If we want to update the street_edge_priority table at 3am every day, we need to figure out how much time there
    // is b/w now and the next 3am, then we can set the update interval to be 24 hours. So we make a calendar object for
    // right now, and one for 3am today. If it is after 3am right now, we set the 3am object to be 3am tomorrow. Then we
    // get the time difference between the 3am object and now.

    val currentTime: Calendar = Calendar.getInstance
    var timeOfNextUpdate: Calendar = Calendar.getInstance
    timeOfNextUpdate.set(Calendar.HOUR_OF_DAY, 3)
    timeOfNextUpdate.set(Calendar.MINUTE, 0)
    timeOfNextUpdate.set(Calendar.SECOND, 0)

//    println(timeOfNextUpdate.get(Calendar.DAY_OF_MONTH))
    // If already past 3am, set next update to 3am tomorrow.
    if (currentTime.after(timeOfNextUpdate)) {
      timeOfNextUpdate.add(Calendar.HOUR_OF_DAY, 24)
    }
//    println(timeOfNextUpdate.get(Calendar.DAY_OF_MONTH)) // if it is after 3am, this should have just incremented.
    val millisUntilNextupdate: Long = timeOfNextUpdate.getTimeInMillis - currentTime.getTimeInMillis
    val durationToNextUpdate: FiniteDuration = FiniteDuration(millisUntilNextupdate, MILLISECONDS)
//    println(millisUntilNextupdate / 3600000.0) // shows hours until next update

    cancellable = Some(
      context.system.scheduler.schedule(
        durationToNextUpdate,
        24.hour,
        self,
        MyDailyActor.Tick
      )(context.dispatcher)
    )
  }

  override def postStop(): Unit = {
    cancellable.foreach(_.cancel())
    cancellable = None
    super.postStop()
  }

  def receive: Receive = {
    case MyDailyActor.Tick =>
      StreetEdgePriorityTable.recalculateStreetPriority
  }

}

object MyDailyActor {
  val Name = "my-daily-actor"
  def props = Props(new DailyActor)
  case object Tick
}
