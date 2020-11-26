package dev.faustin0.bus.bot.models

import java.time.LocalTime
import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }

import cats.implicits._

sealed trait BusInfoQuery extends Product with Serializable

final case class NextBus(
  stop: String,
  bus: Option[String] = None,
  hour: Option[LocalTime] = None
) extends BusInfoQuery

final case class BusStopInfo(stop: String) extends BusInfoQuery

case object BusInfoQuery {
  private val spaces      = " +".r
  private val busStopName = """^([a-zA-Z ]+)$""".r

  //see https://stackoverflow.com/a/45618412
  private val timeFormatter = new DateTimeFormatterBuilder()
    .appendOptional(DateTimeFormatter.ofPattern("HHmm"))
    .appendOptional(DateTimeFormatter.ofPattern("HH:mm"))
    .toFormatter

  def fromText(textMessage: String): Either[Throwable, BusInfoQuery] =
    textMessage match {
      case busStopName(text) => Right(BusStopInfo(text))
      case query             =>
        tokenize(query) match {
          case stop :: bus :: rawTime :: Nil =>
            extractTimeFromText(rawTime)
              .map(time =>
                NextBus(
                  stop = stop,
                  bus = Option(bus).map(_.trim),
                  hour = Option(time)
                )
              )
          case stop :: bus :: Nil            => Right(NextBus(stop, Option(bus)))
          case stop :: Nil                   => Right(NextBus(stop))
          case _                             =>
            Left(
              new IllegalArgumentException(s"cant extract query from '$textMessage'")
            ) //TODO use ADT
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
