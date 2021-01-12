package dev.faustin0.bus.bot.domain

trait BusInfoAlgebra[F[_]] {
  def getNextBuses(query: NextBusQuery): F[BusInfoResponse]

  def searchBusStopByName(query: BusStopInfo): F[List[BusStopDetails]]
}
