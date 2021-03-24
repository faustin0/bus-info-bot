package dev.faustin0.bus.bot

import canoe.api.{ chatApi, Bot, TelegramClient }
import canoe.models.messages.TelegramMessage
import canoe.models.outgoing.TextContent
import canoe.models.{ CallbackButtonSelected, CallbackQuery }
import cats.effect.{ ExitCode, Fiber, IO, IOApp }
import dev.faustin0.bus.bot.domain.{ Callback, UpdateCallback, UpdateType }
import fs2.Stream
import fs2.concurrent.Queue
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.DurationInt

object Application extends IOApp {
  implicit val logger = Slf4jLogger.getLogger[IO]

  def run(args: List[String]): IO[ExitCode] =
    Resources.create.use { res =>
      Stream
        .emit(res.telegramClient)
        .flatMap(implicit telegramClient =>
          for {
            callbackQueue <- Stream.eval(Queue.unbounded[IO, (Callback, TelegramMessage)])
            resultQueue   <- Stream.eval(Queue.unbounded[IO, (String, TelegramMessage)])
            _             <- Stream.eval(logger.info("Up and running"))

            _ <-
              Bot
                .polling[IO]
                .follow(
                  Scenarios.busStopQueries(res.busInfoApi),
                  Scenarios.helpCommandScenario,
                  Scenarios.startCommandScenario(res.userRepo)
                )
                .collect { case CallbackButtonSelected(_, CallbackQuery(_, _, Some(msg), _, _, Some(data), _)) =>
//                  Callback.fromString(data).map(c => (c, msg))
                  Right[IllegalArgumentException, (Callback, TelegramMessage)](
                    (Callback(UpdateType, UpdateCallback(303, None, None)), msg)
                  )
                }
                .through { s =>
                  s.evalTap(e => e.fold(err => IO(false), msg_and_callback => callbackQueue.offer1(msg_and_callback)))
                    .concurrently(processCallbacks(callbackQueue, resultQueue))
                    .concurrently(consumeFromQueue(resultQueue))

                }
//                .flatMap { e =>
//                  Stream(
//                    Stream.eval(e.fold(err => IO(false), msg_and_callback => callbackQueue.offer1(msg_and_callback))),
//                    processCallbacks(callbackQueue, resultQueue),
//                    consumeFromQueue(resultQueue)
//                  ).parJoinUnbounded
//                }

//            b <- Stream
//                   .eval(e.fold(err => IO(false), msg_and_callback => callbackQueue.offer1(msg_and_callback)))
//                   .concurrently(processCallbacks(callbackQueue, resultQueue))
//                   .concurrently(consumeFromQueue(resultQueue))

          } yield ()
        )
        .compile
        .drain
        .as(ExitCode.Success)
    }

  def processCallbacks(
    callbacks: Queue[IO, (Callback, TelegramMessage)],
    results: Queue[IO, (String, TelegramMessage)]
  ): Stream[IO, (Callback, TelegramMessage)] =
    Stream
      .repeatEval(callbacks.dequeue1)
      .evalTap { case (c, m) =>
        logger
          .info(s"processor dequeuing ${m.messageId}") *> IO
          .sleep(2.seconds)
          .flatMap(_ => IO("processed"))
          .flatMap(e => results.enqueue1((e, m)))
          .start
      }
      .evalTap(e => logger.info(s"processor dequeued ${e._2.messageId}"))

  private def consumeFromQueue(queue: Queue[IO, (String, TelegramMessage)])(implicit tc: TelegramClient[IO]) =
    Stream
      .repeatEval(queue.dequeue1)
      .evalTap(e => logger.info(s"consumer dequeued ${e._2.messageId}"))
      .evalMap { case (toSend, telegramMsg) =>
        telegramMsg.chat.send(
          TextContent(toSend)
        )
      }

//  def sharedTopic(): Stream[IO, Topic[IO, Callback]] =
//    Stream.eval(Topic(Callback(UnknownType, EmptyCallback())))

  // S source ------- telegrma --------------------------
  // P - U ------------------------------ U -----------
  //  S  - 10sec, 10sec, 10sec (10 min)
  //  S  - 2sec -
}
