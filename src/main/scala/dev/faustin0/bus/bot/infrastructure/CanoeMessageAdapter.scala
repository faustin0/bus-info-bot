package dev.faustin0.bus.bot.infrastructure

import canoe.api.models.Keyboard
import dev.faustin0.bus.bot.domain.FailedRequest
import io.circe.Encoder
import io.circe.literal.JsonStringContext
import io.circe.syntax._

case class CanoeMessageData(
  body: String,
  keyboard: Keyboard = Keyboard.Unchanged
)

object CanoeMessageAdapter {

  implicit class CanoeAdapter[B](val b: B) extends AnyVal {

    def toCanoeMessage[C](callbackData: C)(implicit M: CanoeMessage[B], E: Encoder[C]): CanoeMessageData =
      CanoeMessageData(
        body = M.body(b),
        keyboard = M.keyboard(callbackData.asJson.noSpaces)
      )

    def toCanoeMessage(implicit M: CanoeMessage[B]): CanoeMessageData =
      toCanoeMessage(json"{}")
  }

  implicit class CanoeErrAdapter(val b: FailedRequest) extends AnyVal {

    def toCanoeMessage(implicit M: CanoeMessage[FailedRequest]): CanoeMessageData =
      CanoeMessageData(
        body = M.body(b),
        keyboard = M.keyboard(json"{}".asJson.noSpaces)
      )
  }
}
