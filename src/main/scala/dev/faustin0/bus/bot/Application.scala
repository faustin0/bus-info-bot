package dev.faustin0.bus.bot

import canoe.api.Bot
import cats.effect.{ ExitCode, IO, IOApp }
import cats.syntax.all._
import fs2.Stream
import org.typelevel.log4cats.{ Logger, SelfAwareStructuredLogger }
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Application extends IOApp {
  implicit private val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(args: List[String]): IO[ExitCode] =
    Resources.create.use { res =>
      Stream
        .emit(res.telegramClient)
        .flatMap(implicit telegramClient =>
          Stream.eval(logger.info("Up and running")) *>
            Bot
              .polling[IO]
              .follow(
                Scenarios.busStopQueries(res.busInfoApi),
                Scenarios.helpCommandScenario,
                Scenarios.startCommandScenario(res.userRepo)
              )
              .through(Scenarios.updateRequestCallback(res.busInfoApi))
        )
        .compile
        .drain
    }
      .onError(logger.error(_)("App collapsed"))
      .as(ExitCode.Success)

}
