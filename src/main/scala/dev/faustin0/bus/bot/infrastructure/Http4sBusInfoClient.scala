package dev.faustin0.bus.bot.infrastructure

import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, IO, Resource, Sync }
import dev.faustin0.bus.bot.domain._
import io.circe.Decoder
import io.circe.generic.auto._
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

class Http4sBusInfoClient[F[_]: Sync](private val client: Client[F], uri: Uri) extends BusInfoAlgebra[F] {

  override def getNextBuses(query: NextBusQuery): F[BusInfoResponse] = {
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
        case Status.Ok       =>
          resp
            .attemptAs[List[BusInfoJson]]
            .map(parsedResp =>
              parsedResp.map {
                case BusInfoJson(bus, true, h, i)  => NextBus(Bus(bus), Satellite(h), i)
                case BusInfoJson(bus, false, h, i) => NextBus(Bus(bus), Planned(h), i)
              }
            )
            .map(parsedResp => SuccessfulResponse(parsedResp))
            .getOrElse(GeneralFailure())
        case Status.NotFound => Sync[F].pure(MissingBusStop())
        case _               => Sync[F].pure(GeneralFailure())
      }
    }
  }

  override def searchBusStopByName(query: BusStopInfo): F[List[BusStopDetails]] = {
    val req = Request[F](
      method = GET,
      uri = (uri / "bus-stops").withQueryParam("name", query.stop),
      headers = Headers.of(
        Accept(MediaType.application.json)
      )
    )

    client.fetchAs(req)
  }

}

private case class BusInfoJson(
  bus: String,
  satellite: Boolean,
  hour: LocalTime,
  busInfo: String
)

object Http4sBusInfoClient {

  implicit private val BusInfoJsonDecoder: Decoder[BusInfoJson] = deriveDecoder[BusInfoJson]

  def apply(httpClient: Client[IO], uri: Uri): Http4sBusInfoClient[IO] = new Http4sBusInfoClient(httpClient, uri)

  def makeResource(
    host: String,
    executionContext: ExecutionContext
  )(implicit ce: ConcurrentEffect[IO]): Resource[IO, Http4sBusInfoClient[IO]] =
    BlazeClientBuilder[IO](executionContext)
      .withConnectTimeout(7 seconds)
      .withRequestTimeout(7 seconds)
      .resource
      .map(client => ClientLogger(logHeaders = false, logBody = true)(client))
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
