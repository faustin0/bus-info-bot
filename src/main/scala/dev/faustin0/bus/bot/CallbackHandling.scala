package dev.faustin0.bus.bot

import canoe.api.models.Keyboard
import canoe.api.{ callbackQueryApi, chatApi, Bot, Scenario, TelegramClient }
import canoe.models.ChatAction.Typing
import canoe.models._
import canoe.syntax.{ command, _ }
import cats.effect.{ ContextShift, ExitCode, IO, IOApp, Timer }
import cats.implicits._
import cats.{ Applicative, Monad }
import dev.faustin0.bus.bot.models.{ BusInfoQuery, BusStopInfo, NextBus }
import fs2.{ Pipe, Stream }
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.chrisdavenport.log4cats.{ Logger, SelfAwareLogger }

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object CallbackHandling extends IOApp {
  private val token = sys.env("TOKEN")
  private val ec    = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def run(args: List[String]): IO[ExitCode] = {
    val telegramClient = Stream.resource(TelegramClient.global[IO](token))

    Http4sBusInfoClient
      .makeResource("http://bus-app.fware.net/", ec)
      .use { busClient =>
        BotApplication.app(telegramClient, busClient)
      }
  }
}

object BotApplication {

  def app(telegramClient: Stream[IO, TelegramClient[IO]], busClient: BusInfoDSL[IO])(implicit
    cs: ContextShift[IO],
    timer: Timer[IO]
  ): IO[ExitCode] =
    telegramClient
      .flatMap(implicit client =>
        Bot
          .polling[IO]
          .follow(echos, busStopQueries(busClient))
          .through(answerCallbacks)
      )
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

  def busStopQueries[F[_]: TelegramClient: Monad](busInfoClient: BusInfoDSL[F]): Scenario[F, Unit] =
    for {
      rawQuery <- Scenario.expect(textMessage)
      chat      = rawQuery.chat
      _        <- Scenario.eval(chat.setAction(Typing))
      query     = BusInfoQuery.fromText(rawQuery.text)
      response  = query
                    .fold(
                      error => Monad[F].pure(error.toString),
                      q =>
                        for {
                          buses <- busInfoClient.getNextBuses(NextBus("303"))
                          msg    = buses.toString
                        } yield msg
                    )
      x        <- Scenario.eval(response).attempt
      _        <- x.fold(
                    e => Scenario.eval(chat.send(e.toString)),
                    msg => Scenario.eval(chat.send(msg))
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
