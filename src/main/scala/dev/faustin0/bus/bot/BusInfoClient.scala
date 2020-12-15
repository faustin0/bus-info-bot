package dev.faustin0.bus.bot

import cats.Applicative
import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, IO, Resource, Sync }
import dev.faustin0.bus.bot.models.{ BusStopPosition, _ }
import io.circe.generic.auto._
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{ Logger => ClientLogger }
import org.http4s.client.{ Client, JavaNetClientBuilder }
import org.http4s.headers.{ `Content-Type`, Accept }
import org.http4s._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

trait BusInfoDSL[F[_]] {
  def getNextBuses(query: NextBus): F[BusInfoResponse]
  def getBusStopInfo(query: BusStopInfo): F[BusStopDetails] //TODO handle missing bus-stop
}

class InMemoryBusInfoClient[F[_]: Applicative] extends BusInfoDSL[F] {
  override def getNextBuses(query: NextBus): F[BusInfoResponse] = Applicative[F].pure(SuccessfulResponse(Nil))

  override def getBusStopInfo(query: BusStopInfo): F[BusStopDetails] = Applicative[F].pure(
    BusStopDetails(
      code = 303,
      name = "stop name",
      location = "stop location",
      comune = "comune",
      areaCode = 500,
      position = BusStopPosition(x = 1, y = 2, lat = 1.2f, long = 1.1f)
    )
  )
}

class Http4sBusInfoClient[F[_]: Sync](private val client: Client[F], uri: Uri) extends BusInfoDSL[F] {

  override def getNextBuses(query: NextBus): F[BusInfoResponse] = {
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
            .attemptAs[List[BusInfo]]
            .map(parsedResp => SuccessfulResponse(parsedResp))
            .getOrElse(GeneralFailure())
        case Status.NotFound => Sync[F].pure(MissingBusStop())
        case _               => Sync[F].pure(GeneralFailure())
      }
    }
  }

  override def getBusStopInfo(query: BusStopInfo): F[BusStopDetails] = {
    val req = Request[F](
      method = GET,
      uri = uri / "bus-stops" / query.stop / "info",
      headers = Headers.of(
        Accept(MediaType.application.json)
      )
    )

    client.fetchAs(req)
  }

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
