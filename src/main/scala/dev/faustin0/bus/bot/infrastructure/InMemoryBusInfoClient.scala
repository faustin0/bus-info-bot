package dev.faustin0.bus.bot.infrastructure

import cats.Applicative
import dev.faustin0.bus.bot.domain._

class InMemoryBusInfoClient[F[_]: Applicative] extends BusInfoAlgebra[F] {

  override def getNextBuses(query: NextBusQuery): F[Either[FailedRequest, NextBusResponse]] =
    Applicative[F].pure(Right(NextBusResponse(Nil)))

  override def searchBusStopByName(query: BusStopInfo): F[Either[FailedRequest, BusStopDetailsResponse]] =
    Applicative[F].pure(
      Right(
        BusStopDetailsResponse(
          List(
            BusStopDetails(
              BusStop(303),
              name = "stop name",
              location = "stop location",
              comune = "comune",
              areaCode = 500,
              position = BusStopPosition(x = 1, y = 2, lat = 1.2f, long = 1.1f)
            )
          )
        )
      )
    )
}
