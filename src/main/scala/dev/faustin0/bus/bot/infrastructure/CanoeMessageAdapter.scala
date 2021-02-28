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

//case class CanoeMessageDataV2[M](
//  content: MessageContent[M],
//  keyboard: Keyboard = Keyboard.Unchanged
//)

object CanoeMessageAdapter {

  implicit class CanoeAdapter[B](private val b: B) extends AnyVal {

    def toCanoeMessage[C](callbackData: C)(implicit M: CanoeTextMessage[B], E: Encoder[C]): CanoeMessageData =
      CanoeMessageData(
        body = M.body(b),
        keyboard = M.keyboard(b, callbackData.asJson.noSpaces)
      )

    def toCanoeMessage(implicit M: CanoeTextMessage[B]): CanoeMessageData =
      toCanoeMessage(json"{}")
  }

  implicit class CanoeErrAdapter(val b: FailedRequest) extends AnyVal {

    def toCanoeMessage(implicit M: CanoeTextMessage[FailedRequest]): CanoeMessageData =
      CanoeMessageData(
        body = M.body(b),
        keyboard = M.keyboard(b, json"{}".asJson.noSpaces)
      )
  }
}
