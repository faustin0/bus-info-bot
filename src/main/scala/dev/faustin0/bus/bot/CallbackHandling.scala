package dev.faustin0.bus.bot

import canoe.api.{ callbackQueryApi, chatApi, Bot, Scenario, TelegramClient }
import canoe.models.ChatAction.Typing
import canoe.models._
import canoe.syntax._
import cats.effect.{ ContextShift, ExitCode, IO, IOApp, Timer }
import cats.implicits._
import cats.{ Applicative, Monad }
import dev.faustin0.bus.bot.infrastructure.Http4sBusInfoClient
import dev.faustin0.bus.bot.domain.BusInfoResponse.CanoeAdapter
import dev.faustin0.bus.bot.domain.CanoeMessageFormats._
import dev.faustin0.bus.bot.domain._
import fs2.{ Pipe, Stream }

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object CallbackHandling extends IOApp {
  private val token = sys.env("TOKEN")
  private val ec    = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def run(args: List[String]): IO[ExitCode] = {
    val telegramClient = Stream.resource(TelegramClient[IO](token, ec))

    Http4sBusInfoClient
      .makeResource("http://bus-app.fware.net/", ec)
      .use { busClient =>
        BotApplication.app(telegramClient, busClient)
      }
  }
}

object BotApplication {

  def app(telegramClient: Stream[IO, TelegramClient[IO]], busClient: BusInfoAlgebra[IO])(implicit
    cs: ContextShift[IO],
    timer: Timer[IO]
  ): IO[ExitCode] =
    telegramClient
      .flatMap(implicit client =>
        Bot
          .polling[IO]
          .follow(busStopQueries(busClient))
          .through(answerCallbacks)
      )
      .compile
      .drain
      .as(ExitCode.Success)

  def busStopQueries[F[_]: TelegramClient: Monad](busInfoClient: BusInfoAlgebra[F]): Scenario[F, Unit] =
    for {
      rawQuery <- Scenario.expect(textMessage)
      chat      = rawQuery.chat
      _        <- Scenario.eval(chat.setAction(Typing))
      query     = BusInfoQuery.fromText(rawQuery.text)
      response  = query match {
                    case q: NextBusQuery =>
                      for {
                        resp <- busInfoClient.getNextBuses(q).map {
                                  case r: SuccessfulResponse => r.toCanoeMessage
                                  case r: GeneralFailure     => r.toCanoeMessage
                                  case r: BadRequest         => r.toCanoeMessage
                                  case r: MissingBusStop     => r.toCanoeMessage
                                  case r: BusStopDetails     => r.toCanoeMessage
                                }
                      } yield resp
                    case q: BusStopInfo  => Monad[F].pure(CanoeMessageData(q.toString)) //todo
                    case q: Malformed    => Monad[F].pure(CanoeMessageData(q.toString)) //todo
                  }

      x <- Scenario.eval(response).attempt
      _ <- x.fold(
             e => Scenario.eval(chat.send(e.toString)),
             msg => Scenario.eval(chat.send(msg.body, keyboard = msg.keyboard))
           )
    } yield ()

  def answerCallbacks[F[_]: Monad: TelegramClient]: Pipe[F, Update, Update] =
    _.evalTap {
      case CallbackButtonSelected(_, query) =>
        query.data match {
          case Some(cbd) =>
            for {
              _ <- query.message.traverse(_.chat.send(cbd))
              _ <- query.finish
            } yield ()
          case _         => Applicative[F].unit
        }
      case _                                => Applicative[F].unit
    }
}
