package dev.faustin0.bus.bot.domain

import cats.implicits._
import io.circe.Decoder.Result
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser._
import io.circe.{ Decoder, DecodingFailure, HCursor }

import java.time.LocalTime

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
