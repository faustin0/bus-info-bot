package dev.faustin0.bus.bot

import canoe.api.models.Keyboard.Inline
import canoe.api.{ callbackQueryApi, chatApi, messageApi, Bot, Scenario, TelegramClient }
import canoe.models.ChatAction.Typing
import canoe.models._
import canoe.syntax._
import cats.effect.{ ContextShift, ExitCode, IO, IOApp, Sync, Timer }
import cats.implicits._
import cats.{ Applicative, Monad }
import dev.faustin0.bus.bot.domain._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageAdapter._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageFormats._
import dev.faustin0.bus.bot.infrastructure.{ CanoeMessageData, Http4sBusInfoClient }
import fs2.{ Pipe, Stream }
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

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
  implicit def logger[F[_]: Sync] = Slf4jLogger.getLogger[F] //FIXME "unsafe" logger

  def app(telegramClient: Stream[IO, TelegramClient[IO]], busClient: BusInfoApi[IO])(implicit
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
//      .handleErrorWith(t => Logger[IO].error(t)("Failure running bot scenarios "))
      .as(ExitCode.Success)

  def busStopQueries[F[_]: TelegramClient: Monad](busInfoClient: BusInfoApi[F]): Scenario[F, Unit] =
    for {
      rawQuery   <- Scenario.expect(textMessage)
      chat        = rawQuery.chat
      _          <- Scenario.eval(chat.setAction(Typing))
      query       = BusInfoQuery.fromText(rawQuery.text)
      waitingMsg <- Scenario.eval(chat.send(s"Richiesta $query"))
      response    = query match {
                      case q: NextBusQuery =>
                        for {
                          resp <- busInfoClient.getNextBuses(q)
                          x     = resp.fold(
                                    failure => CanoeMessageData(""),
                                    success => CanoeMessageData("")
                                  )
                        } yield x
                      case q: BusStopInfo  => Monad[F].pure(CanoeMessageData(q.toString)) //todo
                      case q: Malformed    => Monad[F].pure(CanoeMessageData(q.toString)) //todo
                    }

      msg <- Scenario.eval(response).handleErrorWith { t =>
               Scenario.pure(GeneralFailure().toCanoeMessage)
             }
      _   <- Scenario.eval {
               for {
                 _ <- waitingMsg.editText(msg.body)
                 _ <- msg.keyboard match { //todo clean up this mess
                        case Inline(markup) => waitingMsg.editReplyMarkup(Some(markup))
                        case _              => Monad[F].pure(Left(false))
                      }
               } yield ()
             }
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
