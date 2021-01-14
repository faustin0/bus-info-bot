package dev.faustin0.bus.bot.domain

import java.time.LocalTime

final case class Bus(code: String) extends AnyVal

sealed trait HourOfArrival
final case class Satellite(hour: LocalTime) extends HourOfArrival
final case class Planned(hour: LocalTime)   extends HourOfArrival

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
  bus: Bus,
  hourOfArrival: HourOfArrival,
  additionalInfo: String
)

case class BusStopPosition(
  x: Long,
  y: Long,
  lat: Float,
  long: Float
)
