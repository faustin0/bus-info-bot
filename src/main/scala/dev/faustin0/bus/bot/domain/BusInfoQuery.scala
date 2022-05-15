package dev.faustin0.bus.bot.domain

import cats.implicits._
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }

import java.time.LocalTime
import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }

sealed trait BusInfoQuery extends Product with Serializable

final case class NextBusQuery( // TODO change name
  stop: String,                // fixme this should be a int
  bus: Option[String] = None,
  hour: Option[LocalTime] = None
) extends BusInfoQuery

final case class BusStopInfo(stop: String)       extends BusInfoQuery
final case class Malformed(exception: Throwable) extends BusInfoQuery

case object BusInfoQuery {
  private val spaces      = " +".r
  private val busStopName = """^([a-zA-Z ]+)$""".r

  // see https://stackoverflow.com/a/45618412
  private val timeFormatter = new DateTimeFormatterBuilder
    .appendOptional(DateTimeFormatter.ofPattern("HHmm"))
    .appendOptional(DateTimeFormatter.ofPattern("H:mm"))
    .appendOptional(DateTimeFormatter.ofPattern("HH:mm"))
    .toFormatter

  def fromText(textMessage: String): BusInfoQuery =
    textMessage match {
      case busStopName(text) => BusStopInfo(text)
      case query             =>
        tokenize(query) match {
          case stop :: bus :: rawTime :: Nil =>
            extractTimeFromText(rawTime)
              .map(time =>
                NextBusQuery(
                  stop = stop,
                  bus = Option(bus).map(_.trim),
                  hour = Option(time)
                )
              )
              .fold(ex => Malformed(ex), n => n)
          case stop :: bus :: Nil            => NextBusQuery(stop, Option(bus))
          case stop :: Nil                   => NextBusQuery(stop)
          case _                             => Malformed(new IllegalArgumentException(s"cant extract query from '$textMessage'"))
        }
    }

  private def tokenize(query: String): List[String] =
    query
      .split(spaces.regex)
      .toList
      .map(_.trim)

  private def extractTimeFromText(time: String): Either[RuntimeException, LocalTime] =
    Either
      .catchNonFatal(LocalTime.parse(time, timeFormatter))
      .leftMap(ex => new RuntimeException(s"failed to parse hour: $time", ex))

}

object Codecs {
  implicit val NextBusQueryEncoder: Encoder[NextBusQuery] = deriveEncoder[NextBusQuery]
  implicit val NextBusQueryDecoder: Decoder[NextBusQuery] = deriveDecoder[NextBusQuery]
}
