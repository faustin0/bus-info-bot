package dev.faustin0.bus.bot

import canoe.api.TelegramClient
import cats.effect.{ ContextShift, IO, Resource }
import cats.implicits._
import dev.faustin0.bus.bot.domain.{ BusInfoApi, UserRepository }
import dev.faustin0.bus.bot.infrastructure.{ DynamoUserRepository, Http4sBusInfoClient }
import org.typelevel.log4cats.Logger

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Resources {

  private val telegramToken = IO(sys.env("TOKEN"))
  private val ec            = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def create(implicit cs: ContextShift[IO], logger: Logger[IO]): Resource[IO, Resources] =
    for {
      telegramClient <- Resource.eval(telegramToken).flatMap(t => TelegramClient[IO](t, ec))
      busInfoApi     <- Http4sBusInfoClient.makeResource("http://bus-app.fware.net/", ec)
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
