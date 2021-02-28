package dev.faustin0.bus.bot.infrastructure

import canoe.api.models.Keyboard.Inline
import dev.faustin0.bus.bot.domain.{ Bus, _ }
import dev.faustin0.bus.bot.infrastructure.CanoeMessageAdapter._
import dev.faustin0.bus.bot.infrastructure.CanoeMessageFormats._
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalTime

class CanoeTextMessageFormatsTest extends AnyFunSuite {

  test("Should format a successful message with planned hour") {

    val msg = IncomingBuses(
      BusStop(303),
      Some(Bus("27")),
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

    val msg = IncomingBuses(
      BusStop(303),
      Some(Bus("27")),
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

    val msg = IncomingBuses(
      BusStop(303),
      Some(Bus("27")),
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

  test("Should format a NextBusResponse message ") {

    val msg = IncomingBuses(
      BusStop(303),
      Some(Bus("27")),
      List(NextBus(BusStop(303), Bus("27"), Satellite(LocalTime.of(23, 0)), ""))
    )

    val actual = msg.toCanoeMessage

    assert(actual.body === """|🚏 303
                              |🚌 27
                              |🕐 23:00
                              |🛰 Orario da satellite
                              |""".stripMargin)
  }

  test("Should format an NoMoreBus message ") {

    val msg = NoMoreBus(
      BusStop(303),
      Some(Bus("85"))
    )

    val actual = msg.toCanoeMessage

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

  test("Should format a BusStopDetails response message ") {

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
    assert(
      actual.body ===
        """|🚏 IRNERIO
           |🔢 <code>303</code>
           |📌 <a href='https://www.google.com/maps/search/?api=1&query=45.3,12.4'>BOLOGNA: VIA IRNERIO FR 20/C</a>
           |
           |🚏 IRNERIO
           |🔢 <code>304</code>
           |📌 <a href='https://www.google.com/maps/search/?api=1&query=44.4,11.3'>BOLOGNA: VIA IRNERIO 18</a>
           |""".stripMargin
    )
  }

  test("Should format a BusStopDetails empty response message ") {
    val msg = BusStopDetailsResponse(Nil)

    val actual = msg.toCanoeMessage
    assert(
      actual.body === """🚏 Nessuna fermata trovata"""
    )
  }

  test("Should format a Help message") {

    val actual = HelpResponse.toCanoeMessage

    assert(
      actual.body ===
        s"""
           |Di seguito alcuni esempi, ricorda: l'ordine è importante.
           |
           |richiedere i prossimi bus 28 in arrivo alla fermata 303:
           |<code>303 28</code>
           |
           |richiedere i prossimi bus in arrivo alla fermata 3345:
           |<code>3345</code>
           |
           |richiedere i prossimi bus 28 in arrivo alla fermata 3345 dalle ore 9.30:
           |<code>3345 28 9:30</code>
           |
           |informazioni generali sulla fermata Irnerio:
           |<code>Irnerio</code>
           |""".stripMargin
    )
  }

  test("Should format a Start message") {

    val actual = StartResponse(
      User(
        id = 42,
        firstName = "expected_name",
        lastName = "lastName",
        userName = "username",
        language = None
      )
    ).toCanoeMessage

    assert(
      actual.body ===
        """👋 Ciao expected_name! Benvenuto/a su TperBoBot!
          |
          |🚌 Puoi chiedere un bus specificando:
          |<code>numero_fermata numero_bus</code>
          |
          |🚏 oppure conoscere i bus in arrivo in una fermata:
          |<code>numero_fermata</code>
          |
          |❓ Per altri esempi puoi consulare la sezione "help" tramite il comando /help
          |""".stripMargin
    )
  }
}
