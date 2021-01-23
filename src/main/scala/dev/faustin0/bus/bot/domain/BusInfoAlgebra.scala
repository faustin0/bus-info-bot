package dev.faustin0.bus.bot.domain

trait BusInfoAlgebra[F[_]] {
  def getNextBuses(query: NextBusQuery): F[Either[FailedRequest, NextBusResponse]]

  def searchBusStopByName(query: BusStopInfo): F[Either[FailedRequest, BusStopDetailsResponse]]
}
