package dev.faustin0.bus.bot.infrastructure

import canoe.api.models.Keyboard
import canoe.models.{ InlineKeyboardButton, InlineKeyboardMarkup }
import dev.faustin0.bus.bot.domain.Emoji._
import dev.faustin0.bus.bot.domain.{ FailedRequest, _ }

trait CanoeMessage[M] {
  def keyboard(callbackData: String): Keyboard
  def body(a: M): String
}

object CanoeMessageFormats {

  implicit object NextBusMessage extends CanoeMessage[NextBusResponse] {

    override def body(a: NextBusResponse): String = a match {
      case NoMoreBus(requestedStop, requestedBus) =>
        s"""|🚏 ${requestedStop.code}
            |🚌 ${requestedBus.map(_.code).getOrElse("*")}
            |🕐 Nessun' altra corsa di 85 per la fermata 303
            |""".stripMargin

      case IncomingBuses(requestedStop, requestedBus, info) =>
        info.map { nb =>
          val msg = s"""|$BUS_STOP ${nb.busStop.code}
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

  implicit object FailMessage extends CanoeMessage[FailedRequest] {
    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged

    override def body(a: FailedRequest): String = a match {
      case GeneralFailure() => "Errore nella gestione della richiesta"
      case BadRequest()     => "Errore nei dati inseriti, /help?"
      case MissingBusStop() => "Nessuna fermata trovata"
    }
  }

  implicit object DetailsMessage extends CanoeMessage[BusStopDetailsResponse] {
    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged
    override def body(a: BusStopDetailsResponse): String  = a.toString //todo
  }
}
