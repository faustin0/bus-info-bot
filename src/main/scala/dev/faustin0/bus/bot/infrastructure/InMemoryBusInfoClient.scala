package dev.faustin0.bus.bot.infrastructure

import cats.Applicative
import dev.faustin0.bus.bot.domain._

class InMemoryBusInfoClient[F[_]: Applicative] extends BusInfoAlgebra[F] {
  override def getNextBuses(query: NextBusQuery): F[BusInfoResponse] = Applicative[F].pure(SuccessfulResponse(Nil))

  override def searchBusStopByName(query: BusStopInfo): F[List[BusStopDetails]] = Applicative[F].pure(
    List(
      BusStopDetails(
        code = 303,
        name = "stop name",
        location = "stop location",
        comune = "comune",
        areaCode = 500,
        position = BusStopPosition(x = 1, y = 2, lat = 1.2f, long = 1.1f)
      )
    )
  )
}
