package dev.faustin0.bus.bot.domain

import java.time.LocalTime

final case class Bus(code: String)     extends AnyVal
final case class BusStop(code: String) extends AnyVal

sealed abstract class HourOfArrival(val hour: LocalTime)
final case class Satellite(realTime: LocalTime) extends HourOfArrival(realTime)
final case class Planned(prevision: LocalTime)  extends HourOfArrival(prevision)

sealed trait BusInfoResponse extends Product with Serializable {}

final case class SuccessfulResponse(
  info: List[NextBus]
) extends BusInfoResponse {}

final case class GeneralFailure() extends BusInfoResponse {}

final case class BadRequest() extends BusInfoResponse {}

final case class MissingBusStop() extends BusInfoResponse {}

final case class BusStopDetails(
  code: Int,
  name: String,
  location: String,
  comune: String,
  areaCode: Int,
  position: BusStopPosition
) extends BusInfoResponse {}

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

object NextBus {

  def apply(busStop: BusStop, bus: Bus, hourOfArrival: HourOfArrival, additionalInfo: String): NextBus =
    new NextBus(busStop, bus, hourOfArrival, Option(additionalInfo).filter(_.nonEmpty))
}
