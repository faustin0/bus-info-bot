package dev.faustin0.bus.bot.models

import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._

import java.time.LocalTime

sealed trait BusInfoResponse extends Product with Serializable

final case class SuccessfulResponse(
  info: List[BusInfo]
) extends BusInfoResponse

final case class GeneralFailure() extends BusInfoResponse
final case class BadRequest()     extends BusInfoResponse
final case class MissingBusStop() extends BusInfoResponse

case class BusInfo(
  bus: String,
  satellite: Boolean,
  hour: LocalTime,
  busInfo: String
)

case class BusStopDetails(
  code: Int,
  name: String,
  location: String,
  comune: String,
  areaCode: Int,
  position: BusStopPosition
)

case class BusStopPosition(
  x: Long,
  y: Long,
  lat: Float,
  long: Float
)

object BusInfoResponse {

  implicit private val BusInfoDecoder: Decoder[BusInfo] = deriveDecoder[BusInfo]

  def fromJson(json: Json): Either[IllegalArgumentException, BusInfo] = //TODO change exception
    json
      .as[BusInfo]
      .leftMap(t => new IllegalArgumentException(s"failed to parse json: $json", t))
}
