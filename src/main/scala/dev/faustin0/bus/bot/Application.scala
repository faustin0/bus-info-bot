package dev.faustin0.bus.bot

import canoe.api.Bot
import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import fs2.Stream
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Application extends IOApp {
  implicit val logger = Slf4jLogger.getLogger[IO]

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
        .as(ExitCode.Success)
    }

}
