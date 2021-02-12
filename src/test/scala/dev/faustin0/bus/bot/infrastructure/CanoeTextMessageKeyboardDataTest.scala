package dev.faustin0.bus.bot.infrastructure

import canoe.models.{ ForceReply, InlineKeyboardMarkup, ReplyKeyboardMarkup, ReplyKeyboardRemove }
import dev.faustin0.bus.bot.domain.Codecs.NextBusQueryEncoder
import dev.faustin0.bus.bot.domain.{ Bus, _ }
import dev.faustin0.bus.bot.infrastructure.CanoeMessageAdapter._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageFormats._
import io.circe.literal.JsonStringContext
import org.scalatest.Inside
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.LocalTime

class CanoeTextMessageKeyboardTest extends AnyFunSuite with Inside with Matchers {

  test("should contain the expected keyboard data for a IncomingBuses message") {

    val msg: NextBusResponse = IncomingBuses(
      BusStop(303),
      Some(Bus("27")),
      List(
        NextBus(BusStop(303), Bus("27"), Planned(LocalTime.of(23, 0)), "additional info")
      )
    )

    val actual = msg.toCanoeMessage(NextBusQuery("303", Some("27"), Some(LocalTime.of(23, 0))))
    //fixme "stop" : "303" should be an int
    inside(actual.keyboard.replyMarkup) { case Some(keyboard) =>
      inside(keyboard) { case InlineKeyboardMarkup(Seq(Seq(button))) =>
        button.text shouldBe "update"
        button.callbackData should contain(json"""
            {
              "stop" : "303", 
              "bus" : "27",
              "hour" : "23:00:00"
            }
            """.noSpaces)
      }
    }
  }

}
