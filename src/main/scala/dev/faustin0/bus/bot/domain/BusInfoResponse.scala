package dev.faustin0.bus.bot.domain

import java.time.LocalTime

final case class Bus(code: String)  extends AnyVal
final case class BusStop(code: Int) extends AnyVal

sealed trait HourOfArrival { def hour: LocalTime }
final case class Satellite(hour: LocalTime) extends HourOfArrival
final case class Planned(hour: LocalTime)   extends HourOfArrival

sealed trait FailedRequest        extends Product with Serializable
final case class GeneralFailure() extends FailedRequest
final case class BadRequest()     extends FailedRequest
final case class MissingBusStop() extends FailedRequest

sealed trait NextBusResponse extends Product with Serializable {
  def requestedStop: BusStop
  def requestedBus: Option[Bus]
}

final case class IncomingBuses(
  requestedStop: BusStop,
  requestedBus: Option[Bus],
  info: List[NextBus] //todo non empty list
) extends NextBusResponse

final case class NoMoreBus(
  requestedStop: BusStop,
  requestedBus: Option[Bus]
) extends NextBusResponse

final case class BusStopDetailsResponse(
  busStops: List[BusStopDetails]
)

final case class BusStopDetails(
  busStop: BusStop,
  name: String,
  location: String,
  comune: String,
  areaCode: Int,
  position: BusStopPosition
)

case class NextBus(
  busStop: BusStop,
  bus: Bus,
  hourOfArrival: HourOfArrival,
  additionalInfo: Option[String]
)

case class BusStopPosition(
  x: Long,
  y: Long,
  lat: Float,
  long: Float
)

case object HelpResponse {}

object NextBus {

  def apply(busStop: BusStop, bus: Bus, hourOfArrival: HourOfArrival, additionalInfo: String): NextBus =
    new NextBus(busStop, bus, hourOfArrival, Option(additionalInfo).filter(_.nonEmpty))
}
