package dev.faustin0.bus.bot

import canoe.api.TelegramClient
import cats.effect.{ IO, Resource }
import dev.faustin0.bus.bot.domain.{ BusInfoApi, UserRepository }
import dev.faustin0.bus.bot.infrastructure.{ DynamoUserRepository, Http4sBusInfoClient }
import org.typelevel.log4cats.Logger

object Resources {

  def create(implicit logger: Logger[IO]): Resource[IO, Resources] =
    for {
      config         <- Resource.eval(Config.load)
      telegramClient <- TelegramClient[IO](config.telegramToken)
      busInfoApi     <- Http4sBusInfoClient.makeResource(config.busAppUri)
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
