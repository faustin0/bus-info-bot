package dev.faustin0.bus.bot

import cats.Applicative
import cats.effect.{ Blocker, ConcurrentEffect, ContextShift, IO, Resource, Sync }
import dev.faustin0.bus.bot.models.BusInfoResponse
import io.circe.generic.auto._
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{ Logger => ClientLogger }
import org.http4s.client.{ Client, JavaNetClientBuilder }
import org.http4s.headers.{ `Content-Type`, Accept }
import org.http4s.{ Headers, MediaType, Request, Uri }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

trait BusInfoDSL[F[_]] {
  def getNextBuses: F[List[BusInfoResponse]]
  def getBusStopInfo: F[String]
}

class InMemoryBusInfoClient[F[_]: Applicative] extends BusInfoDSL[F] {
  override def getNextBuses: F[List[BusInfoResponse]] = Applicative[F].pure(Nil)

  override def getBusStopInfo: F[String] = Applicative[F].pure("")
}

class Http4sBusInfoClient[F[_]: Sync](private val client: Client[F], uri: Uri) extends BusInfoDSL[F] {

  override def getNextBuses: F[List[BusInfoResponse]] = {
    val req = Request[F](
      method = GET,
      uri = (uri / "bus-stops" / "303")
        .withQueryParams(
          Map(
            "bus" -> "28"
          )
        ),
      headers = Headers.of(
        Accept(MediaType.application.json),
        `Content-Type`(MediaType.application.json)
      )
    )
    Sync[F].suspend(client.expect[List[BusInfoResponse]](req))
  }

  override def getBusStopInfo: F[String] = {
    val req = Request[F](
      method = GET,
      uri = uri.withPath("/health"),
      headers = Headers.of(
        Accept(MediaType.application.json)
      )
    )

    Sync[F].suspend(client.expect[String](req))
  }

}

object Http4sBusInfoClient {

  def apply(httpClient: Client[IO], uri: Uri): Http4sBusInfoClient[IO] = new Http4sBusInfoClient(httpClient, uri)

  def makeWithResource(
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
