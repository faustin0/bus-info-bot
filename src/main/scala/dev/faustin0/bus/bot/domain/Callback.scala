package dev.faustin0.bus.bot.domain

import cats.implicits._
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.parser._
import io.circe.syntax.EncoderOps

import java.time.LocalTime
import java.time.format.DateTimeFormatter

final case class Callback(`type`: CallbackType, body: Payload)

sealed trait CallbackType extends Product with Serializable {
  val value: String
}

case object UpdateType extends CallbackType {
  override val value = "updateRequest"
}

case object FollowType extends CallbackType {
  override val value = "followRequest"
}

object CallbackType {

  def fromString(t: String): Either[IllegalArgumentException, CallbackType] =
    t match {
      case UpdateType.value => Right(UpdateType)
      case FollowType.value => Right(FollowType)
      case _                => Left(new IllegalArgumentException(s"Invalid callback type '$t'"))
    }
}

sealed trait Payload extends Product with Serializable

final case class FollowCallback(
  busStop: Int,
  bus: Option[String] = None,
  hour: Option[LocalTime] = None
) extends Payload

final case class UpdateCallback(
  busStop: Int,
  bus: Option[String] = None,
  hour: Option[LocalTime] = None
) extends Payload

object Callback {

  def fromString(data: String)(implicit D: Decoder[Callback]): Either[IllegalArgumentException, Callback] =
    decode[Callback](data)
      .leftMap(e => new IllegalArgumentException(s"Failure decoding callback $data", e))

  def toJsonString(callback: Callback)(implicit E: Encoder[Callback]): String =
    E(callback).noSpaces

}

case object decodersInstances {

  implicit lazy val updateCallbackDecoder: Decoder[UpdateCallback] = deriveDecoder
  implicit lazy val followCallbackDecoder: Decoder[FollowCallback] = deriveDecoder

  implicit object typeDecoder extends Decoder[CallbackType] {

    def apply(c: HCursor): Result[CallbackType] =
      c.downField("type")
        .as[String]
        .flatMap(t =>
          CallbackType
            .fromString(t)
            .leftMap(e => DecodingFailure.fromThrowable(e, c.history))
        )
  }

  implicit def payloadDecoder(implicit D: Decoder[CallbackType]): Decoder[Payload] = D.flatMap {
    case UpdateType => cursor => cursor.downField("body").as[UpdateCallback]
    case FollowType => cursor => cursor.downField("body").as[FollowCallback]
  }

  implicit def CallbackDecoder(implicit
    typeDecoder: io.circe.Decoder[CallbackType],
    payloadDecoder: io.circe.Decoder[Payload]
  ): Decoder[Callback] = cursor =>
    for {
      callbackType <- typeDecoder(cursor)
      payload      <- payloadDecoder(cursor)
    } yield Callback(callbackType, payload)

}

object encodersInstances {
  import io.circe.literal.JsonStringContext

  private lazy val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  implicit lazy val updateCallbackEncoder: Encoder[UpdateCallback] = deriveEncoder
  implicit lazy val followCallbackEncoder: Encoder[FollowCallback] = deriveEncoder

  implicit lazy val timeEncoder: Encoder[LocalTime] = (a: LocalTime) => {
    a.format(timeFormatter).asJson
  }

  implicit object typeEncoder extends Encoder[CallbackType] {
    override def apply(callbackType: CallbackType): Json = Json.fromString(callbackType.value)
  }

  implicit def payloadEncoder: Encoder[Payload] = {
    case payload: FollowCallback => payload.asJson.dropNullValues
    case payload: UpdateCallback => payload.asJson.dropNullValues
  }

  implicit def CallbackEncoder(implicit
    typeDecoder: Encoder[CallbackType],
    payloadDecoder: Encoder[Payload]
  ): Encoder[Callback] = { (callback: Callback) =>
    json"""
          {
            "type": ${typeDecoder(callback.`type`)},
            "body": ${payloadDecoder(callback.body)}
          }
          """
  }

}
