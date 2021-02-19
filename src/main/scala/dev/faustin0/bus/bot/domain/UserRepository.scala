package dev.faustin0.bus.bot.domain

trait UserRepository[F[_]] {
  def create(user: User): F[Unit]
  def get(id: Int): F[Option[User]]
}
