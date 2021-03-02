package dev.faustin0.bus.bot

import _root_.fs2._
import canoe.api.{ Bot, TelegramClient }
import canoe.methods.Method
import canoe.methods.updates.GetUpdates
import canoe.models.messages.TextMessage
import canoe.models.{ MessageReceived, PrivateChat, Update }
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.Ignore
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime

@Ignore
class ScenariosTest extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  implicit val tc = new TelegramClient[IO] {

    override def execute[Req, Res](request: Req)(implicit M: Method[Req, Res]): IO[Res] =
      if (M.name != GetUpdates.method.name) throw new UnsupportedOperationException(M.name)
      else {
        val getUpdates: GetUpdates = request.asInstanceOf[GetUpdates]
        val update: Update         =
          MessageReceived(getUpdates.offset.get, TextMessage(-1, PrivateChat(-1, None, None, None), -1, ""))
        IO.pure(List(update).asInstanceOf[Res])
      }

//      implicit val dec = M.decoder
//      implicit val enc = M.encoder
//      IO.fromEither(
//        decode[Res](request.asJson.noSpaces)
//      )
  }

  type Message = String
  type ChatId  = Int

  def updates(messages: List[(Message, ChatId)]): Stream[IO, Update] =
    Stream
      .emits(messages.zipWithIndex.map { case ((m, id), i) =>
        MessageReceived(i, TextMessage(i, PrivateChat(id, None, None, None), -1, m))
      })
      .metered[IO](0.2.seconds)

  "todo" in {
    val messages: List[(Message, ChatId)] = List(
      "/help" -> 1,
      "/help" -> 2
    )

    val helpScenario = Scenarios.helpCommandScenario

    val bot = Bot.fromStream(updates(messages))
    bot.follow(helpScenario).compile.toList.unsafeRunSync()
//      .map(list =>
//        list.collect { case MessageReceived(_, m: TextMessage) =>
//          m.text
//        }
//      )
//      .asserting(texts => assert(texts.nonEmpty))
    IO(assert(true))
  }
}

object TestIO {
  import cats.effect.{ ContextShift, IO, Timer }
  import fs2.Stream

  import scala.concurrent.ExecutionContext

  implicit class IOStreamOps[A](stream: Stream[IO, A]) {
    def toList(): List[A] = stream.compile.toList.unsafeRunSync()

    def value(): A = toList().head

    def count(): Int = toList().size

    def run(): Unit = stream.compile.drain.unsafeRunSync()
  }

  implicit val globalContext: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val globalTimer: Timer[IO]          = IO.timer(ExecutionContext.global)
}
