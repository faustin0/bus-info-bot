package dev.faustin0.bus.bot.infrastructure

import canoe.models.{ InlineKeyboardMarkup, ReplyKeyboardMarkup }
import dev.faustin0.bus.bot.domain.Codecs.NextBusQueryEncoder
import dev.faustin0.bus.bot.domain._
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
    // fixme "stop" : "303" should be an int
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

  test("should contain the expected keyboard data for a BusStopDetailsResponse message") {

    val msg = BusStopDetailsResponse(
      busStops = List(
        BusStopDetails(
          busStop = BusStop(303),
          name = "IRNERIO",
          location = "VIA IRNERIO FR 20/C",
          comune = "BOLOGNA",
          areaCode = 500,
          position = BusStopPosition(1, 2, 45.3f, 12.4f)
        ),
        BusStopDetails(
          busStop = BusStop(304),
          name = "IRNERIO",
          location = "VIA IRNERIO 18",
          comune = "BOLOGNA",
          areaCode = 500,
          position = BusStopPosition(5, 6, 44.4f, 11.3f)
        )
      )
    )

    val actual = msg.toCanoeMessage
    inside(actual.keyboard.replyMarkup) { case Some(keyboard) =>
      inside(keyboard) { case ReplyKeyboardMarkup(Seq(Seq(b1), Seq(b2)), Some(true), Some(true), None) =>
        b1.text shouldBe "303"
        b2.text shouldBe "304"
      }
    }
  }

}
