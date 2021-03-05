package dev.faustin0.bus.bot.domain

import dev.faustin0.bus.bot.domain.decodersInstances._
import dev.faustin0.bus.bot.domain.encodersInstances._
import io.circe.literal.JsonStringContext
import org.scalatest.Inside
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.LocalTime

class CallbackTest extends AnyFunSuite with Inside {

  test("should parse an UpdateRequest callback") {
    val callbackStr = json"""
        {
          "type": "updateRequest",
          "body": {
            "busStop": 303,
            "bus": "28",  
            "hour": "23:00"
          }
        }
        """.noSpaces

    val value = Callback.fromString(callbackStr)

    inside(value) { case Right(callback) =>
      callback shouldBe Callback(
        UpdateType,
        UpdateCallback(303, Some("28"), Some(LocalTime.of(23, 0)))
      )
    }
  }

  test("should parse a Follow callback") {
    val callbackStr = json"""
        {
          "type": "followRequest",
          "body": {
            "busStop": 303
          }
        }
        """.noSpaces

    val value = Callback.fromString(callbackStr)

    inside(value) { case Right(callback) =>
      callback shouldBe Callback(
        FollowType,
        FollowCallback(303)
      )
    }
  }

  test("should encode a update callback") {
    val toEncode = Callback(
      UpdateType,
      UpdateCallback(303, Some("28"), Some(LocalTime.of(23, 0)))
    )

    Callback.toJsonString(toEncode) shouldBe json"""
        {
          "type": "updateRequest",
          "body": {
            "busStop": 303,
            "bus": "28",  
            "hour": "23:00"
          }
        }
        """.noSpaces
  }

  test("should encode a follow callback") {
    val toEncode = Callback(
      FollowType,
      FollowCallback(303)
    )

    Callback.toJsonString(toEncode) shouldBe json"""
        {
          "type": "followRequest",
          "body": {
            "busStop": 303
          }
        }
        """.noSpaces
  }
  test("decoding and encoding should lead to the original input") {
    val startingPoint = Callback(
      FollowType,
      FollowCallback(303)
    )

    val encoded = Callback.toJsonString(startingPoint)
    val decoded = Callback.fromString(encoded)

    inside(decoded) { case Right(callback) =>
      callback shouldBe startingPoint
    }
  }

}
