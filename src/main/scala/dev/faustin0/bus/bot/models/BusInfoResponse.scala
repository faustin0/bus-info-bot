package dev.faustin0.bus.bot.models

import java.time.LocalTime

import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._

case class BusInfoResponse(
  bus: String,
  satellite: Boolean,
  hour: LocalTime,
  busInfo: String
)

object BusInfoResponses {

  implicit private val BusInfoResponseDecoder: Decoder[BusInfoResponse] =
    deriveDecoder[BusInfoResponse]

  def fromJson(json: Json): Either[IllegalArgumentException, BusInfoResponse] = //TODO change exception
    json
      .as[BusInfoResponse]
      .leftMap(t => new IllegalArgumentException(s"failed to parse json: $json", t))
}
