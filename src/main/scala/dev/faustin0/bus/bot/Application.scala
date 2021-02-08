package dev.faustin0.bus.bot

import canoe.api.models.Keyboard.Inline
import canoe.api.{ callbackQueryApi, chatApi, messageApi, Bot, Scenario, TelegramClient }
import canoe.models.ChatAction.Typing
import canoe.models.outgoing.TextContent
import canoe.models.{ CallbackButtonSelected, Update, _ }
import canoe.syntax._
import cats.Monad
import cats.effect.{ ContextShift, ExitCode, IO, IOApp, Sync, Timer }
import cats.implicits._
import dev.faustin0.bus.bot.domain.Codecs._
import dev.faustin0.bus.bot.domain._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageAdapter._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageFormats._
import dev.faustin0.bus.bot.infrastructure.Http4sBusInfoClient
import fs2.{ Pipe, Stream }
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.parser.decode

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
  implicit def unsafeLogger = Slf4jLogger.getLogger[IO]

  def app(telegramClient: Stream[IO, TelegramClient[IO]], busClient: BusInfoApi[IO])(implicit
    cs: ContextShift[IO],
    timer: Timer[IO]
  ): IO[ExitCode] =
    telegramClient
      .flatMap(implicit client =>
        Bot
          .polling[IO]
          .follow(busStopQueries(busClient))
          .through(answerCallbacks(busClient))
      )
      .compile
      .drain
      .as(ExitCode.Success)

  def busStopQueries[F[_]: TelegramClient: Sync](busInfoClient: BusInfoApi[F]): Scenario[F, Unit] =
    for {
      logger   <- Scenario.eval(Slf4jLogger.create[F])
      rawQuery <- Scenario.expect(textMessage)
      chat      = rawQuery.chat
      query     = BusInfoQuery.fromText(rawQuery.text)
      response  = query match {
                    case q: NextBusQuery => nextBusScenario(q, chat, busInfoClient)
                    case q: BusStopInfo  => detailsScenario(q, chat, busInfoClient)
                    case q: Malformed    => Scenario.done[F] //todo CanoeMessageData(q.toString)
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
      waitingMsg     <- Scenario.eval(chat.send(q.toCanoeMessage.body))
      _              <- Scenario.eval(chat.setAction(Typing))
      requestOutcome <- Scenario.eval(busInfoClient.getNextBuses(q))
      msgData         = requestOutcome.fold(
                          failure => failure.toCanoeMessage,
                          success => success.toCanoeMessage(q)
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

  def answerCallbacks(
    busInfoClient: BusInfoApi[IO]
  )(implicit telegramClient: TelegramClient[IO]): Pipe[IO, Update, Update] =
    mainStream =>
      mainStream.evalTap {
        case CallbackButtonSelected(_, query) =>
          query.data match {
            case Some(callBackData) =>
              for {
                chat          <- IO.fromOption(query.message.map(_.chat))(new IllegalStateException("no chat in callback"))
                queryToUpdate <- IO.fromEither(decode[NextBusQuery](callBackData))
                queryUpdate   <- busInfoClient.getNextBuses(queryToUpdate)
                msgData        = queryUpdate.fold(
                                   failure => failure.toCanoeMessage,
                                   success => success.toCanoeMessage(queryToUpdate)
                                 )
                _             <- chat.send(msgData.body, keyboard = msgData.keyboard)
                _             <- query.inform(s"Richiesta aggiornata")
              } yield ()
            case _                  => IO.unit
          }
        case _                                => IO.unit
      }.handleErrorWith(t => Stream.eval(IO(println(t))).flatMap(_ => mainStream)) //todo use logger

}
