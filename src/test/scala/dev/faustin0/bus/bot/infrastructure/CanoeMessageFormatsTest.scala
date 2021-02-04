package dev.faustin0.bus.bot.infrastructure

import canoe.api.models.Keyboard.Inline
import dev.faustin0.bus.bot.domain.{ Bus, _ }
import dev.faustin0.bus.bot.infrastructure.CanoeMessageAdapter._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageFormats._
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalTime

class CanoeMessageFormatsTest extends AnyFunSuite {

  test("Should format a successful message with planned hour") {

    val msg: NextBusResponse = IncomingBuses(
      BusStop(303),
      Some(Bus("27")),
      List(
        NextBus(BusStop(303), Bus("27"), Planned(LocalTime.of(23, 0)), "additional info")
      )
    )
    val actual               = msg.toCanoeMessage

    assert(actual.body === """|🚏 303
                              |🚌 27
                              |🕐 23:00
                              |⚠ Bus con orario previsto
                              |ℹ additional info
                              |""".stripMargin)
  }

  test("Should format a successful message with satellite hour") {

    val msg: NextBusResponse = IncomingBuses(
      BusStop(303),
      Some(Bus("27")),
      List(
        NextBus(BusStop(303), Bus("27"), Satellite(LocalTime.of(23, 0)), "")
      )
    )
    val actual               = msg.toCanoeMessage

    assert(actual.body === """|🚏 303
                              |🚌 27
                              |🕐 23:00
                              |🛰 Orario da satellite
                              |""".stripMargin)
  }

  test("Successful message should have an update inline keyboard") {

    val msg: NextBusResponse = IncomingBuses(
      BusStop(303),
      Some(Bus("27")),
      List(
        NextBus(BusStop(303), Bus("27"), Satellite(LocalTime.of(23, 0)), ""),
        NextBus(BusStop(303), Bus("27"), Satellite(LocalTime.of(23, 30)), "")
      )
    )
    val actual               = msg.toCanoeMessage

    actual.keyboard match {
      case Inline(markup) =>
        markup.inlineKeyboard match {
          case Seq(Seq(button)) => assert(button.text === "update", "keyboard should have update button")
        }
    }
  }

  test("Should format a NextBusResponse message ") {

    val msg: NextBusResponse = IncomingBuses(
      BusStop(303),
      Some(Bus("27")),
      List(NextBus(BusStop(303), Bus("27"), Satellite(LocalTime.of(23, 0)), ""))
    )
    val actual               = msg.toCanoeMessage

    assert(actual.body === """|🚏 303
                              |🚌 27
                              |🕐 23:00
                              |🛰 Orario da satellite
                              |""".stripMargin)
  }

  test("Should format an NoMoreBus message ") {

    val msg: NextBusResponse = NoMoreBus(
      BusStop(303),
      Some(Bus("85"))
    )
    val actual               = msg.toCanoeMessage

    assert(actual.body === """|🚏 303
                              |🚌 85
                              |🕐 Nessun' altra corsa di 85 per la fermata 303
                              |""".stripMargin)
  }

  test("Should format a BadRequest message, with a help hint") {

    val actual = BadRequest().toCanoeMessage

    assert(actual.body === "Errore nei dati inseriti, /help?")
  }

  test("Should format a GeneralFailure message") {

    val actual = GeneralFailure().toCanoeMessage

    assert(actual.body === "Errore nella gestione della richiesta")
  }

  test("Should format a MissingBusStop message") {

    val actual = MissingBusStop().toCanoeMessage

    assert(actual.body === "Nessuna fermata trovata")
  }

  ignore("Should format a BusStopDetails response message ") {

    val msg    = BusStopDetailsResponse(
      busStops = List(
        BusStopDetails(
          busStop = BusStop(303),
          name = "IRNERIO",
          location = "VIA IRNERIO FR 20/C",
          comune = "BOLOGNA",
          areaCode = 500,
          position = BusStopPosition(1, 2, 3, 4)
        ),
        BusStopDetails(
          busStop = BusStop(304),
          name = "IRNERIO",
          location = "VIA IRNERIO 18",
          comune = "BOLOGNA",
          areaCode = 500,
          position = BusStopPosition(5, 6, 7, 8)
        )
      )
    )
    val actual = msg.toCanoeMessage

    assert(actual.body === """|🚏 IRNERIO 303 
                              |BOLOGNA : VIA IRNERIO FR 20/C
                              |
                              |🚏 IRNERIO 304 
                              |BOLOGNA : VIA IRNERIO 18
                              |""".stripMargin)
  }
}
