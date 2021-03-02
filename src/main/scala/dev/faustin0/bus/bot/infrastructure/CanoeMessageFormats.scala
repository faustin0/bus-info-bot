package dev.faustin0.bus.bot.infrastructure

import canoe.api.models.Keyboard
import canoe.models._
import dev.faustin0.bus.bot.domain.Emoji._
import dev.faustin0.bus.bot.domain.{ FailedRequest, _ }

trait CanoeTextMessage[-M] {
  def body(a: M): String
  def keyboard(a: M, callbackData: String): Keyboard
}

object CanoeMessageFormats {

//  private lazy val HTML = Some(ParseMode.HTML)

  implicit object NextBusMessage extends CanoeTextMessage[NextBusResponse] {

    override def body(a: NextBusResponse): String =
      a match {
        case NoMoreBus(requestedStop, requestedBus) =>
          val requestedBusCode = requestedBus.map(_.code).getOrElse("*")
          s"""|$BUS_STOP ${requestedStop.code}
              |$BUS $requestedBusCode
              |$CLOCK Nessun' altra corsa di $requestedBusCode per la fermata ${requestedStop.code}
              |""".stripMargin

        case IncomingBuses(_, _, info) =>
          info.map { nb =>
            val msg =
              s"""|$BUS_STOP ${nb.busStop.code}
                  |$BUS ${nb.bus.code}
                  |$CLOCK ${nb.hourOfArrival.hour}
                  |${nb.hourOfArrival match {
                case Satellite(_) => s"$SATELLITE Orario da satellite"
                case Planned(_)   => s"$WARN Bus con orario previsto"
              }}""".stripMargin

            nb.additionalInfo
              .map(info => s"$INFO $info")
              .map(info => s"""|$msg
                               |$info
                               |""".stripMargin)
              .getOrElse(msg)
          }
            .map(_.trim)
            .map(s => s.concat(System.lineSeparator()))
            .mkString(System.lineSeparator())
      }

    override def keyboard(a: NextBusResponse, callbackData: String): Keyboard = {
      val button = InlineKeyboardButton.callbackData(text = "update", cbd = callbackData)
      val markup = InlineKeyboardMarkup.singleButton(button)
      Keyboard.Inline(markup)
    }
  }

  implicit object FailMessage extends CanoeTextMessage[FailedRequest] {

    override def body(a: FailedRequest): String = a match {
      case GeneralFailure() => "Errore nella gestione della richiesta"
      case BadRequest()     => "Errore nei dati inseriti, /help?"
      case MissingBusStop() => "Nessuna fermata trovata"
    }

    override def keyboard(a: FailedRequest, callbackData: String): Keyboard = Keyboard.Unchanged
  }

  implicit object DetailsMessage extends CanoeTextMessage[BusStopDetailsResponse] {

    override def body(a: BusStopDetailsResponse): String = {
      val stops = a.busStops.map { detail =>
        s"""|$BUS_STOP ${detail.name}
            |$NUM <code>${detail.busStop.code}</code>
            |$POINT ${makeHtmlMapsUrl(detail)}
            |""".stripMargin
      }
      stops
        .map(_.trim)
        .map(s => s.concat(System.lineSeparator()))
        .reduceOption((s1, s2) => s1 + System.lineSeparator() + s2)
        .getOrElse(s"$BUS_STOP Nessuna fermata trovata")
    }

    override def keyboard(a: BusStopDetailsResponse, callbackData: String): Keyboard = {

      val buttons = a.busStops
        .map(bs => bs.busStop.code)
        .map(text => KeyboardButton.text(text.toString))

      val markup = ReplyKeyboardMarkup.singleColumn(
        buttons,
        resizeKeyboard = Some(true),
        oneTimeKeyboard = Some(true)
      )
      Keyboard.Reply(markup)
    }

    private def makeHtmlMapsUrl(detail: BusStopDetails) =
      s"""<a href='https://www.google.com/maps/search/?api=1&query=${detail.position.lat},${detail.position.long}'>${detail.comune}: ${detail.location}</a>"""
  }

  implicit object WaitingMessage extends CanoeTextMessage[NextBusQuery] {

    override def body(q: NextBusQuery): String =
      s"""
         |Richiesta: 
         |$BUS_STOP ${q.stop}
         |$BUS ${q.bus.getOrElse("*")}
         |$CLOCK ${q.hour.getOrElse("in arrivo")}
         |""".stripMargin

    override def keyboard(a: NextBusQuery, callbackData: String): Keyboard = Keyboard.Unchanged
  }

  implicit object MalformedMessage extends CanoeTextMessage[Malformed] {
    override def body(a: Malformed): String = "Errore nei dati inseriti,  /help ?"

    override def keyboard(a: Malformed, callbackData: String): Keyboard = Keyboard.Unchanged
  }

  implicit object HelpMessage extends CanoeTextMessage[HelpResponse.type] {

    override def body(a: HelpResponse.type): String =
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

    override def keyboard(a: HelpResponse.type, callbackData: String): Keyboard = Keyboard.Unchanged
  }

  implicit object StartMessage extends CanoeTextMessage[StartResponse] {

    override def body(a: StartResponse): String =
      s"""$WAVE Ciao ${a.user.firstName}! Benvenuto/a su TperBoBot!
         |
         |$BUS Puoi chiedere un bus specificando:
         |<code>numero_fermata numero_bus</code>
         |<code>303 28</code>
         |
         |$BUS_STOP oppure conoscere i bus in arrivo in una fermata:
         |<code>numero_fermata</code>
         |<code>303</code>
         |
         |$BUS_STOP puoi inoltre cercare il numero della fermata e altre informazioni, dato il suo nome:
         |<code>nome_fermata</code>
         |<code>Irnerio</code>
         |
         |Per qualsiasi problema, idea o suggerimento puoi contattarmi @FaustoDiNatale
         |
         |$QUESTION Per altri esempi puoi consulare la sezione "help" tramite il comando /help
         |""".stripMargin

    override def keyboard(a: StartResponse, callbackData: String): Keyboard = Keyboard.Unchanged
  }
}
