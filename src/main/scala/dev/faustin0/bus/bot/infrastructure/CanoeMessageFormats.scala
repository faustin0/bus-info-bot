package dev.faustin0.bus.bot.infrastructure

import canoe.api.models.Keyboard
import canoe.models.{ InlineKeyboardButton, InlineKeyboardMarkup }
import cats.Show
import dev.faustin0.bus.bot.domain.Emoji._
import dev.faustin0.bus.bot.domain._

trait CanoeMessage[M] {
  def keyboard(callbackData: String): Keyboard
  def body(a: M): String
}

object CanoeMessageFormats {

  implicit object SuccessMessage extends CanoeMessage[SuccessfulResponse] {

    override def body(a: SuccessfulResponse): String = a.info.map { nb =>
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
      .mkString(System.lineSeparator())

    override def keyboard(callbackData: String): Keyboard = {
      val button = InlineKeyboardButton.callbackData(text = "update", cbd = callbackData)
      val markup = InlineKeyboardMarkup.singleButton(button)
      Keyboard.Inline(markup)
    }
  }

  implicit object FailMessage extends CanoeMessage[GeneralFailure] {
    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged
    override def body(a: GeneralFailure): String          = a.toString //todo
  }

  implicit object BadMessage extends CanoeMessage[BadRequest] {
    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged
    override def body(a: BadRequest): String              = a.toString //todo
  }

  implicit object MissingMessage extends CanoeMessage[MissingBusStop] {
    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged
    override def body(a: MissingBusStop): String          = a.toString //todo
  }

  implicit object DetailsMessage extends CanoeMessage[BusStopDetails] {
    override def keyboard(callbackData: String): Keyboard = Keyboard.Unchanged
    override def body(a: BusStopDetails): String          = a.toString //todo
  }
}
