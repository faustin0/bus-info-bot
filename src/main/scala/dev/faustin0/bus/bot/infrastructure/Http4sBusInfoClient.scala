package dev.faustin0.bus.bot.infrastructure

import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, IO, Resource, Sync }
import dev.faustin0.bus.bot.domain._
import dev.faustin0.bus.bot.infrastructure.JsonSchema.{ BusInfoJson, BusStopDetailsJson }
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.Method.GET
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{ Logger => ClientLogger }
import org.http4s.client.{ Client, JavaNetClientBuilder }
import org.http4s.headers.{ `Content-Type`, Accept }

import java.time.LocalTime
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class Http4sBusInfoClient[F[_]: Sync](private val client: Client[F], uri: Uri) extends BusInfoApi[F] {

  override def getNextBuses(query: NextBusQuery): F[Either[FailedRequest, NextBusResponse]] = {
    val req = Request[F](
      method = GET,
      uri = (uri / "bus-stops" / query.stop)
        .withOptionQueryParam("bus", query.bus)
        .withOptionQueryParam("hour", query.hour.map(_.formatted("HH:mm"))),
      headers = Headers.of(
        Accept(MediaType.application.json),
        `Content-Type`(MediaType.application.json)
      )
    )

    client.run(req).use { resp =>
      resp.status match {
        case Status.Ok         =>
          resp
            .attemptAs[List[BusInfoJson]]
            .map(parsedResp =>
              parsedResp.map {
                //todo, replace query.stop with information from json
                case BusInfoJson(bus, true, h, i)  =>
                  NextBus(BusStop(query.stop.toInt), Bus(bus), Satellite(h), i)
                case BusInfoJson(bus, false, h, i) =>
                  //todo, replace query.stop with information from json
                  NextBus(BusStop(query.stop.toInt), Bus(bus), Planned(h), i)
              }
            )
            .map(parsedResp => Right(NextBusResponse(parsedResp)))
            .getOrElse(Left(GeneralFailure()))
        case Status.NotFound   => Sync[F].pure(Left(MissingBusStop()))
        case Status.BadRequest => Sync[F].pure(Left(BadRequest()))
        case _                 => Sync[F].pure(Left(GeneralFailure()))
      }
    }
  }

  override def searchBusStopByName(query: BusStopInfo): F[Either[FailedRequest, BusStopDetailsResponse]] = {
    val req = Request[F](
      method = GET,
      uri = (uri / "bus-stops").withQueryParam("name", query.stop),
      headers = Headers.of(
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
        case Status.NotFound   => Sync[F].pure(Left(MissingBusStop()))
        case Status.BadRequest => Sync[F].pure(Left(BadRequest()))
        case _                 => Sync[F].pure(Left(GeneralFailure()))
      }
    }
  }

}

private object JsonSchema {
  implicit lazy val BusInfoDecoder: Decoder[BusInfoJson]               = deriveDecoder[BusInfoJson]
  implicit lazy val BusStopDetailsDecoder: Decoder[BusStopDetailsJson] = deriveDecoder[BusStopDetailsJson]

  case class BusInfoJson(
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
    position: BusStopPosition
  )

  case class PositionJson(
    x: Long,
    y: Long,
    lat: Float,
    long: Float
  )
}

object Http4sBusInfoClient {

  def apply(httpClient: Client[IO], uri: Uri): Http4sBusInfoClient[IO] = new Http4sBusInfoClient(httpClient, uri)

  def makeResource(
    host: String,
    executionContext: ExecutionContext
  )(implicit ce: ConcurrentEffect[IO]): Resource[IO, Http4sBusInfoClient[IO]] =
    BlazeClientBuilder[IO](executionContext)
      .withConnectTimeout(7 seconds)
      .withRequestTimeout(7 seconds)
      .resource
      .map(client => ClientLogger(logHeaders = true, logBody = true)(client))
      .map(client => new Http4sBusInfoClient(client, Uri.unsafeFromString(host)))

  def make(
    host: String,
    executionContext: ExecutionContext
  )(implicit ce: ContextShift[IO]): Http4sBusInfoClient[IO] = {
    val blocker      = Blocker.liftExecutionContext(executionContext)
    val httpClient   = JavaNetClientBuilder[IO](blocker).create
    val loggedClient = ClientLogger(logHeaders = false, logBody = true)(httpClient)

    new Http4sBusInfoClient(loggedClient, Uri.unsafeFromString(host))
  }
}
