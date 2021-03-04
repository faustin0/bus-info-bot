package dev.faustin0.bus.bot.domain

import dev.faustin0.bus.bot.domain.decodersInstances._
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

}
