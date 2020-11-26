package dev.faustin0.bus.bot

import cats.Applicative
import dev.faustin0.bus.bot.models.BusInfoResponse

trait BusInfoClient[F[_]] {
  def getNextBuses: F[List[BusInfoResponse]]
  def getBusStopInfo: F[String]
}

class InMemoryBusInfoClient[F[_]: Applicative] extends BusInfoClient[F] {
  override def getNextBuses: F[List[BusInfoResponse]] = Applicative[F].pure(Nil)

  override def getBusStopInfo: F[String] = Applicative[F].pure("")
}
