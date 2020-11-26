package dev.faustin0.bus.bot

import canoe.api.models.Keyboard
import canoe.api.{ callbackQueryApi, chatApi, Bot, Scenario, TelegramClient }
import canoe.models.ChatAction.Typing
import canoe.models._
import canoe.syntax.{ command, _ }
import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import cats.{ Applicative, Monad }
import dev.faustin0.bus.bot.models.BusInfoQuery
import fs2.{ Pipe, Stream }

object CallbackHandling extends IOApp {
  val token: String = sys.env("TOKEN")

  def run(args: List[String]): IO[ExitCode] =
    Stream
      .resource(TelegramClient.global[IO](token))
      .flatMap { implicit client =>
        Bot
          .polling[IO]
          .follow(echos, busStopQueries(new InMemoryBusInfoClient()))
          .through(answerCallbacks)
      }
      .compile
      .drain
      .as(ExitCode.Success)

  val inlineBtn = InlineKeyboardButton.callbackData(text = "button", cbd = "callback data")

  val inlineKeyboardMarkUp = InlineKeyboardMarkup.singleButton(inlineBtn)
  val keyboardMarkUp       = Keyboard.Inline(inlineKeyboardMarkUp)

  def echos[F[_]: TelegramClient]: Scenario[F, Unit] =
    for {
      msg <- Scenario.expect(command("callback"))
      _   <- Scenario.eval(msg.chat.send(content = "pretty message", keyboard = keyboardMarkUp))
    } yield ()

  def busStopQueries[F[_]: TelegramClient](busInfoClient: BusInfoClient[F]): Scenario[F, Unit] =
    for {
      rawQuery <- Scenario.expect(textMessage)
      chat      = rawQuery.chat
      _        <- Scenario.eval(chat.setAction(Typing))
      query     = BusInfoQuery.fromText(rawQuery.text)
//      response  =
//        query
//          .fold(
//            error => Scenario.pure(error.toString),
//            msg => Scenario.eval(busInfoClient.getNextBuses())
//          )
      _        <- Scenario.eval(chat.send(s"matched a query $query"))
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
