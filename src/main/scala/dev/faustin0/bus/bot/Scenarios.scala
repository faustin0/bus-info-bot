package dev.faustin0.bus.bot

import canoe.api.models.Keyboard.Inline
import canoe.api.{ callbackQueryApi, chatApi, messageApi, Scenario, TelegramClient }
import canoe.models.ChatAction.Typing
import canoe.models.messages.{ TelegramMessage, TextMessage }
import canoe.models.outgoing.TextContent
import canoe.models.{ CallbackButtonSelected, Chat, ParseMode, Update }
import canoe.syntax._
import cats.Monad
import cats.effect.{ IO, Sync }
import cats.implicits._
import dev.faustin0.bus.bot.domain.Codecs._
import dev.faustin0.bus.bot.domain._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageAdapter._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageFormats._
import fs2.Pipe
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.parser.decode

object Scenarios {
  implicit private val logger = Slf4jLogger.getLogger[IO] //todo leave this here ?

  def busStopQueries(busInfoClient: BusInfoApi[IO])(implicit tc: TelegramClient[IO]): Scenario[IO, Unit] =
    for {
      rawQuery <- Scenario.expect(textNotCommand)
      chat      = rawQuery.chat
      query     = BusInfoQuery.fromText(rawQuery.text)
      response  = query match {
                    case q: NextBusQuery => nextBusScenario(q, chat, busInfoClient)
                    case q: BusStopInfo  => detailsScenario(q, chat, busInfoClient)
                    case q: Malformed    => malformedQueryScenario(q, chat)
                  }
      _        <- response.handleErrorWith { ex =>
                    for {
                      _ <- Scenario.eval(logger.error(ex)(s"Failure for query: '$rawQuery'"))
                      _ <- Scenario.eval(chat.send("something broke"))
                    } yield ()
                  }
    } yield ()

  private def textNotCommand: PartialFunction[TelegramMessage, TextMessage] = {
    case m: TextMessage if !m.text.startsWith("/") => m
  }

  private def malformedQueryScenario[F[_]: TelegramClient](q: Malformed, chat: Chat): Scenario[F, TextMessage] =
    Scenario.eval(chat.send(q.toCanoeMessage.body))

  def helpCommandScenario(implicit tc: TelegramClient[IO]): Scenario[IO, Unit] =
    for {
      message <- Scenario.expect(command("help"))
      _       <- Scenario.eval(logger.info(s"help requested: ${message.from}"))
      _       <- Scenario.eval(
                   message.chat.send(
                     TextContent(HelpResponse.toCanoeMessage.body, parseMode = Some(ParseMode.HTML))
                   )
                 )
    } yield ()

  def startCommandScenario(userRepo: UserRepository[IO])(implicit tc: TelegramClient[IO]): Scenario[IO, Unit] = {
    val scenario = for {
      message   <- Scenario.expect(command("start"))
      from      <- Scenario.eval(IO.fromOption(message.from)(new IllegalStateException("start command without user")))
      user       = User.fromTelegramUser(from)
      _         <- Scenario.eval(
                     message.chat.send(
                       TextContent(StartResponse(user).toCanoeMessage.body, Some(ParseMode.HTML))
                     )
                   )
      maybeUser <- Scenario.eval(userRepo.get(user.id))
      _         <- Scenario.eval(maybeUser.fold(ifEmpty = userRepo.create(user))(_ => IO.unit))
    } yield ()

    scenario.handleErrorWith { t =>
      Scenario.eval(logger.error(t)("start command scenario"))
    }
  }

  private def detailsScenario[F[_]: TelegramClient](
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
                         chat.send(
                           TextContent(
                             text = msgData.body,
                             parseMode = Some(ParseMode.HTML),
                             disableWebPagePreview = Some(true)
                           )
                         )
                       }
    } yield ()

  private def nextBusScenario[F[_]: TelegramClient: Monad](
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

  def updateRequestCallback(
    busInfoClient: BusInfoApi[IO]
  )(implicit telegramClient: TelegramClient[IO]): Pipe[IO, Update, Update] = {

    def handler(update: Update): IO[Unit] =
      update match {
        case CallbackButtonSelected(_, query) =>
          query.data.map { callBackData =>
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
          }.getOrElse(IO.unit)
        case _                                => IO.unit
      }

    mainStream =>
      mainStream.evalTap { u =>
        handler(u)
          .handleErrorWith(t => logger.error(t)("update request callback"))
      }
  }
}
