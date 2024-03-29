package dev.faustin0.bus.bot.infrastructure

import cats.effect.{ IO, Resource }
import dev.faustin0.bus.bot.domain._
import dev.faustin0.bus.bot.infrastructure.JsonSchema.{ BusInfoJson, BusStopDetailsJson }
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.Method.GET
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.middleware.{ Logger => ClientLogger }
import org.http4s.client.{ Client, JavaNetClientBuilder }
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.{ `Content-Type`, Accept }

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.DurationInt

class Http4sBusInfoClient(private val client: Client[IO], uri: Uri) extends BusInfoApi[IO] {
  private lazy val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  override def getNextBuses(query: NextBusQuery): IO[Either[FailedRequest, NextBusResponse]] = {
    val req = Request[IO](
      method = GET,
      uri = (uri / "bus-stops" / query.stop)
        .withOptionQueryParam("bus", query.bus)
        .withOptionQueryParam("hour", query.hour.map(_.format(dateTimeFormatter))),
      headers = Headers(
        Accept(MediaType.application.json),
        `Content-Type`(MediaType.application.json)
      )
    )

    client.run(req).use { resp =>
      resp.status match {
        case Status.Ok         =>
          resp
            .attemptAs[List[BusInfoJson]]
            .map {
              case buses @ List(_, _*) =>
                IncomingBuses(
                  requestedStop = BusStop(query.stop.toInt),
                  query.bus.map(Bus), // todo mina su toInt
                  info = buses.map {
                    case BusInfoJson(busStopCode, bus, true, h, i)  =>
                      NextBus(BusStop(busStopCode), Bus(bus), Satellite(h), i)
                    case BusInfoJson(busStopCode, bus, false, h, i) =>
                      NextBus(BusStop(busStopCode), Bus(bus), Planned(h), i)
                  }
                )
              case _                   =>
                NoMoreBus(
                  requestedStop = BusStop(query.stop.toInt), // todo mina su toInt
                  requestedBus = query.bus.map(Bus)
                )
            }
            .map(parsedResp => Right(parsedResp))
            .getOrElse(Left(GeneralFailure()))
        case Status.NotFound   => IO.pure(Left(MissingBusStop()))
        case Status.BadRequest => IO.pure(Left(BadRequest()))
        case _                 => IO.pure(Left(GeneralFailure()))
      }
    }
  }

  override def searchBusStopByName(query: BusStopInfo): IO[Either[FailedRequest, BusStopDetailsResponse]] = {
    val req = Request[IO](
      method = GET,
      uri = (uri / "bus-stops").withQueryParam("name", query.stop),
      headers = Headers(
        Accept(MediaType.application.json)
      )
    )

    client.run(req).use { resp =>
      resp.status match {
        case Status.Ok         =>
          resp
            .attemptAs[List[BusStopDetailsJson]]
            .map(parsedResp =>
              parsedResp.map(r =>
                BusStopDetails(
                  busStop = BusStop(r.code),
                  name = r.name,
                  location = r.location,
                  comune = r.comune,
                  areaCode = r.areaCode,
                  position = BusStopPosition(r.position.x, r.position.x, r.position.lat, r.position.long)
                )
              )
            )
            .map(details => Right(BusStopDetailsResponse(details)))
            .getOrElse(Left(GeneralFailure()))
        case Status.NotFound   => IO.pure(Left(MissingBusStop()))
        case Status.BadRequest => IO.pure(Left(BadRequest()))
        case _                 => IO.pure(Left(GeneralFailure()))
      }
    }
  }

}

private object JsonSchema {
  implicit lazy val BusInfoDecoder: Decoder[BusInfoJson]               = deriveDecoder[BusInfoJson]
  implicit lazy val PositionJsonDecoder: Decoder[PositionJson]         = deriveDecoder[PositionJson]
  implicit lazy val BusStopDetailsDecoder: Decoder[BusStopDetailsJson] = deriveDecoder[BusStopDetailsJson]

  case class BusInfoJson(
    busStopCode: Int,
    bus: String,
    satellite: Boolean,
    hour: LocalTime,
    busInfo: String
  )

  case class BusStopDetailsJson(
    code: Int,
    name: String,
    location: String,
    comune: String,
    areaCode: Int,
    position: PositionJson
  )

  case class PositionJson(
    x: Long,
    y: Long,
    lat: Float,
    long: Float
  )

}

object Http4sBusInfoClient {

  def apply(httpClient: Client[IO], uri: Uri): Http4sBusInfoClient = new Http4sBusInfoClient(httpClient, uri)

  def makeResource(host: Uri): Resource[IO, Http4sBusInfoClient] =
    EmberClientBuilder
      .default[IO]
      .withTimeout(7 seconds)
      .build
      .map(client => ClientLogger(logHeaders = true, logBody = true)(client))
      .map(client => Http4sBusInfoClient(client, host))

  def make(host: String): Http4sBusInfoClient = {
    val httpClient   = JavaNetClientBuilder[IO].create
    val loggedClient = ClientLogger(logHeaders = false, logBody = true)(httpClient)

    new Http4sBusInfoClient(loggedClient, Uri.unsafeFromString(host))
  }

}
