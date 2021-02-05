package dev.faustin0.bus.bot

import canoe.api.models.Keyboard.Inline
import canoe.api.{ callbackQueryApi, chatApi, messageApi, Bot, Scenario, TelegramClient }
import canoe.models.ChatAction.Typing
import canoe.models._
import canoe.models.outgoing.TextContent
import canoe.syntax._
import cats.effect.{ ContextShift, ExitCode, IO, IOApp, Sync, Timer }
import cats.implicits._
import cats.{ Applicative, Monad }
import dev.faustin0.bus.bot.domain._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageAdapter._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageFormats._
import dev.faustin0.bus.bot.infrastructure.Http4sBusInfoClient
import fs2.{ Pipe, Stream }
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Application extends IOApp {
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

  def busStopQueries[F[_]: TelegramClient: Sync](busInfoClient: BusInfoApi[F]): Scenario[F, Unit] =
    for {
      logger   <- Scenario.eval(Slf4jLogger.create[F])
      rawQuery <- Scenario.expect(textMessage)
      chat      = rawQuery.chat
      query     = BusInfoQuery.fromText(rawQuery.text)
      response  = query match {
                    case q: NextBusQuery => nextBusScenario(q, chat, busInfoClient)

                    case q: BusStopInfo => detailsScenario(q, chat, busInfoClient)

                    case q: Malformed => Scenario.done[F] //todo CanoeMessageData(q.toString)
                  }
      _        <- response.handleErrorWith { ex =>
                    for {
                      _ <- Scenario.eval(logger.error(ex)(s"Failure for query: '$rawQuery'"))
                      _ <- Scenario.eval(chat.send("something broke"))
                    } yield ()
                  }
    } yield ()

  def detailsScenario[F[_]: TelegramClient](
    q: BusStopInfo,
    chat: Chat,
    busInfoClient: BusInfoApi[F]
  ): Scenario[F, Unit] =
    for {
      searchOutcome <- Scenario.eval(busInfoClient.searchBusStopByName(q))
      msgData        = searchOutcome.fold(
                         failure => failure.toCanoeMessage,
                         success => success.toCanoeMessage
                       )
      _             <- Scenario.eval {
                         chat.send(TextContent(msgData.body, parseMode = Some(ParseMode.HTML)))
                       }
    } yield ()

  def nextBusScenario[F[_]: TelegramClient: Monad](
    q: NextBusQuery,
    chat: Chat,
    busInfoClient: BusInfoApi[F]
  ): Scenario[F, Unit] =
    for {
      waitingMsg     <- Scenario.eval(chat.send(s"Richiesta $q"))
      _              <- Scenario.eval(chat.setAction(Typing))
      requestOutcome <- Scenario.eval(busInfoClient.getNextBuses(q))
      msgData         = requestOutcome.fold(
                          failure => failure.toCanoeMessage,
                          success => success.toCanoeMessage
                        )
      _              <- Scenario.eval {
                          for {
                            _ <- waitingMsg.editText(msgData.body)
                            _ <- msgData.keyboard match { //todo clean up this mess
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
