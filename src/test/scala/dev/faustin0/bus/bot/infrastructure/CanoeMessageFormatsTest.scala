package dev.faustin0.bus.bot.infrastructure

import canoe.api.models.Keyboard.Inline
import dev.faustin0.bus.bot.domain.{
  Bus,
  BusStop,
  BusStopDetails,
  BusStopPosition,
  NextBus,
  NextBusResponse,
  Planned,
  Satellite
}
import dev.faustin0.bus.bot.infrastructure.CanoeMessageAdapter.CanoeAdapter
import dev.faustin0.bus.bot.infrastructure.CanoeMessageFormats._
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalTime

class CanoeMessageFormatsTest extends AnyFunSuite {

  test("Should format a successful message with planned hour") {

    val msg    = NextBusResponse(
      List(
        NextBus(BusStop(303), Bus("27"), Planned(LocalTime.of(23, 0)), "additional info")
      )
    )
    val actual = msg.toCanoeMessage

    assert(actual.body === """|🚏 303
                              |🚌 27
                              |🕐 23:00
                              |⚠ Bus con orario previsto
                              |ℹ additional info
                              |""".stripMargin)
  }

  test("Should format a successful message with satellite hour") {

    val msg    = NextBusResponse(
      List(
        NextBus(BusStop(303), Bus("27"), Satellite(LocalTime.of(23, 0)), "")
      )
    )
    val actual = msg.toCanoeMessage

    assert(actual.body === """|🚏 303
                              |🚌 27
                              |🕐 23:00
                              |🛰 Orario da satellite
                              |""".stripMargin)
  }

  test("Successful message should have an update inline keyboard") {

    val msg    = NextBusResponse(
      List(
        NextBus(BusStop(303), Bus("27"), Satellite(LocalTime.of(23, 0)), ""),
        NextBus(BusStop(303), Bus("27"), Satellite(LocalTime.of(23, 30)), "")
      )
    )
    val actual = msg.toCanoeMessage

    actual.keyboard match {
      case Inline(markup) =>
        markup.inlineKeyboard match {
          case Seq(Seq(button)) => assert(button.text === "update", "keyboard should have update button")
        }
    }
  }

  test("Should format a MissingBusStop message ") {

    val msg    = NextBusResponse(
      List(
        NextBus(BusStop(303), Bus("27"), Satellite(LocalTime.of(23, 0)), "")
      )
    )
    val actual = msg.toCanoeMessage

    assert(actual.body === """|🚏 303
                              |🚌 27
                              |🕐 23:00
                              |🛰 Orario da satellite
                              |""".stripMargin)
  }

  ignore("Should format a DetailsMessage message ") {

    val msg    = BusStopDetails(
      busStop = BusStop(303),
      name = "name",
      location = "location",
      comune = "comune",
      areaCode = 500,
      position = BusStopPosition(0, 0, 0, 0)
    )
    val actual = msg.toCanoeMessage

    assert(actual.body === """|🚏 name 303
                              |🕐 23:00
                              |🛰 Orario da satellite
                              |""".stripMargin)
  }
}
