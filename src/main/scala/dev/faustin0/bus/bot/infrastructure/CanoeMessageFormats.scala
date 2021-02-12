package dev.faustin0.bus.bot.infrastructure

import canoe.api.models.Keyboard
import canoe.models.{ InlineKeyboardButton, InlineKeyboardMarkup, ParseMode }
import dev.faustin0.bus.bot.domain.Emoji._
import dev.faustin0.bus.bot.domain.{ FailedRequest, _ }

trait CanoeTextMessage[M] {
  def body(a: M): String
  def keyboard(callbackData: String): Keyboard
}

//trait CanoeTextMessage[M] {
//  def body(a: M): TextContent
//  def keyboard(callbackData: String): Keyboard
//}

object CanoeMessageFormats {

  private lazy val HTML = Some(ParseMode.HTML);

  implicit object NextBusMessage extends CanoeTextMessage[NextBusResponse] {

    override def body(a: NextBusResponse): String =
      a match {
        case NoMoreBus(requestedStop, requestedBus) =>
          val requestedBusCode = requestedBus.map(_.code).getOrElse("*")
          s"""|$BUS_STOP ${requestedStop.code}
              |$BUS $requestedBusCode
              |$CLOCK Nessun' altra corsa di $requestedBusCode per la fermata ${requestedStop.code}
              |""".stripMargin

        case IncomingBuses(requestedStop, requestedBus, info) =>
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

    override def keyboard(callbackData: String): Keyboard = {
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

    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged
  }

  implicit object DetailsMessage extends CanoeTextMessage[BusStopDetailsResponse] {

    override def body(a: BusStopDetailsResponse): String = a.busStops.map { detail =>
      s"""|$BUS_STOP ${detail.name}
          |$NUM <code>${detail.busStop.code}</code>
          |$POINT ${makeHtmlMapsUrl(detail)}
          |""".stripMargin
    }
      .map(_.trim)
      .map(s => s.concat(System.lineSeparator()))
      .mkString(System.lineSeparator())

    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged

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

    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged

  }

  implicit object MalformedMessage extends CanoeTextMessage[Malformed] {
    override def body(a: Malformed): String = "Errore nei dati inseriti,  /help ?"

    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged
  }

  implicit object HelpMessage extends CanoeTextMessage[HelpResponse.type] {

    override def body(a: HelpResponse.type): String =
      s"""
         |Di seguito alcuni esempi, ricorda: l'ordine è importante.
         |richiedere i prossimi bus 28 in arrivo alla fermata 303:
         |<code>303 28</code>
         |richiedere i prossimi bus in arrivo alla fermata 3345:
         |<code>3345</code>
         |richiedere i prossimi bus 28 in arrivo alla fermata 3345 per le ore 9.30:
         |<code>3345 28 0930</code>
         |informazioni generali sulla fermata Irnerio:
         |<code>Irnerio</code>
         |""".stripMargin

    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged
  }
}
