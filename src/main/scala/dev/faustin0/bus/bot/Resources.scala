package dev.faustin0.bus.bot

import canoe.api.TelegramClient
import cats.effect.{ IO, Resource }
import dev.faustin0.bus.bot.domain.{ BusInfoApi, UserRepository }
import dev.faustin0.bus.bot.infrastructure.{ DynamoUserRepository, Http4sBusInfoClient }
import org.typelevel.log4cats.Logger

object Resources {

  private val telegramToken = IO(sys.env("TOKEN"))

  def create(implicit logger: Logger[IO]): Resource[IO, Resources] =
    for {
      telegramClient <- Resource.eval(telegramToken).flatMap(TelegramClient[IO](_))
      busInfoApi     <- Http4sBusInfoClient.makeResource("http://bus-app.fware.net/")
      userRepo       <- DynamoUserRepository.makeResource
      resources       = Resources(
                          telegramClient,
                          busInfoApi,
                          userRepo
                        )
    } yield resources

  case class Resources private (
    telegramClient: TelegramClient[IO],
    busInfoApi: BusInfoApi[IO],
    userRepo: UserRepository[IO]
  )
}
