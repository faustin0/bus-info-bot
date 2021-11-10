package dev.faustin0.bus.bot.domain

import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, Encoder }

import java.time.LocalTime
import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder }
import scala.util.Try

sealed trait BusInfoQuery extends Product with Serializable

final case class NextBusQuery( //TODO change name
  stop: Int,
  bus: Option[String] = None,
  hour: Option[LocalTime] = None
) extends BusInfoQuery

final case class BusStopInfo(stop: String)       extends BusInfoQuery
final case class Malformed(exception: Throwable) extends BusInfoQuery

case object BusInfoQuery {
  private val spaces      = " +".r
  private val busStopName = """^([a-zA-Z ]+)$""".r

  //see https://stackoverflow.com/a/45618412
  private val timeFormatter = new DateTimeFormatterBuilder()
    .appendOptional(DateTimeFormatter.ofPattern("HHmm"))
    .appendOptional(DateTimeFormatter.ofPattern("H:mm"))
    .appendOptional(DateTimeFormatter.ofPattern("HH:mm"))
    .appendOptional(DateTimeFormatter.ofPattern("H:m"))
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
                  stop = stop.toInt, //fixme
                  bus = Option(bus).map(_.trim),
                  hour = Option(time)
                )
              )
              .fold(ex => Malformed(ex), n => n)
          case stop :: bus :: Nil            => NextBusQuery(stop.toInt, Option(bus))
          case stop :: Nil                   => NextBusQuery(stop.toInt)
          case _                             => Malformed(new IllegalArgumentException(s"cant extract query from '$textMessage'"))
        }
    }

  import fastparse._
  import MultiLineWhitespace._

  private def number[_: P]: P[Int]       = P(CharIn("0-9").rep(1).!.map(_.toInt))
  def busStopNameParser[_: P]: P[String] = P(&(" ").flatMap(_ => AnyChar.rep(1).!))
  private def hour[_: P]: P[LocalTime]   = P(&(" ").flatMap(_ => AnyChar.rep(1).!.map(LocalTime.parse(_, timeFormatter))))

  def parseNextBusQuery[_: P]: P[NextBusQuery] = P(number ~ busStopNameParser.? ~ hour.? ~ End).map {
    case (stop, bus, time) =>
      NextBusQuery(
        stop = stop,
        bus = bus,
        hour = time
      )
  }

  private def tokenize(query: String): List[String] =
    query
      .split(spaces.regex)
      .toList
      .map(_.trim)

  private def extractTimeFromText(time: String): Try[LocalTime] =
    Try(LocalTime.parse(time, timeFormatter))

}

object Codecs {
  implicit val NextBusQueryEncoder: Encoder[NextBusQuery] = deriveEncoder[NextBusQuery]
  implicit val NextBusQueryDecoder: Decoder[NextBusQuery] = deriveDecoder[NextBusQuery]
}
