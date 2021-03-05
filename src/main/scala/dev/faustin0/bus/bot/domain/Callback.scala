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

sealed trait CallbackType extends Product with Serializable
case object UpdateType    extends CallbackType
case object FollowType    extends CallbackType

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

    private val supportedTypes = Map(
      "updateRequest" -> UpdateType,
      "followRequest" -> FollowType
    )

    def apply(c: HCursor): Result[CallbackType] =
      c.downField("type")
        .as[String]
        .flatMap(t =>
          supportedTypes
            .get(t)
            .toRight(
              DecodingFailure(
                s"Invalid callback type '$t', supported types: ${supportedTypes.keys}",
                c.history
              )
            )
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

    override def apply(callbackType: CallbackType): Json = callbackType match {
      case UpdateType => Json.fromString("updateRequest")
      case FollowType => Json.fromString("followRequest")
    }
  }

  implicit def payloadEncoder: Encoder[Payload] = {
    case payload: FollowCallback => payload.asJson.dropNullValues
    case payload: UpdateCallback => payload.asJson.dropNullValues
  }

  implicit def CallbackEncoder(implicit
    typeDecoder: Encoder[CallbackType],
    payloadDecoder: Encoder[Payload]
  ): Encoder[Callback] = { (callback: Callback) =>
    typeDecoder(callback.`type`)
    payloadDecoder(callback.body)
    json"""{
            "type": ${typeDecoder(callback.`type`)},
            "body": ${payloadDecoder(callback.body)}
          }"""
  }

}
