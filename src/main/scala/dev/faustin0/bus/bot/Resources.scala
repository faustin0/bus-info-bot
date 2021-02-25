package dev.faustin0.bus.bot

import canoe.api.TelegramClient
import cats.effect.{ ContextShift, IO, Resource }
import dev.faustin0.bus.bot.domain.{ BusInfoApi, UserRepository }
import dev.faustin0.bus.bot.infrastructure.{ DynamoUserRepository, Http4sBusInfoClient }
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Resources {

  private val telegramToken = sys.env("TOKEN")
  private val ec            = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  implicit private val logger: IO[Logger[IO]] = Slf4jLogger.create[IO]

  def create(implicit cs: ContextShift[IO], logger: Logger[IO]): Resource[IO, Resources] =
    for {
      telegramClient <- TelegramClient[IO](telegramToken, ec)
      busInfoApi     <- Http4sBusInfoClient.makeResource("http://bus-app.fware.net/", ec)
      userRepo       <- DynamoUserRepository.makeFromEnv
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
