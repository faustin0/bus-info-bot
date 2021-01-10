package dev.faustin0.bus.bot.models

import canoe.api.models.Keyboard
import canoe.models.{ InlineKeyboardButton, InlineKeyboardMarkup }
import dev.faustin0.bus.bot.models.Emoji.{ BUS, BUS_STOP, CLOCK, WARN }

case class CanoeMessageData(
  body: String,
  keyboard: Keyboard = Keyboard.Unchanged
)

trait CanoeMessage[M] {
  def keyboard(callbackData: String): Keyboard
  def body(a: M): String
}

object CanoeMessageFormats {

  implicit object SuccessMessage extends CanoeMessage[SuccessfulResponse] {

    override def body(a: SuccessfulResponse): String = a.info.map { i =>
      s"""
         |$BUS_STOP todo
         |$BUS ${i.bus}
         |$CLOCK ${i.hourOfArrival match {
        case Satellite(hour) => hour
        case Planned(hour)   => s"$WARN previsto: $hour"
      }}
         |""".stripMargin
    }.mkString(System.lineSeparator())

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
