package dev.faustin0.bus.bot.models

import io.circe.literal.JsonStringContext
import io.circe.syntax.EncoderOps
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class BusInfoResponsesTest extends AnyFunSuite with Matchers {

  test("parse json into a bus info response") {
    val jsonExample = json"""
      {
        "bus": "19",
        "satellite": true,
        "hour": "10:28",
        "busInfo": "(Bus5581 CON PEDANA)"
      }
    """
    val actual      = BusInfo.fromJson(jsonExample)

    actual match {
      case Right(_) => succeed
      case Left(_)  => fail("failed to parse json")
    }
  }

  test("fail case should contain failed json") {
    val actual = BusInfo.fromJson("{bad-json}".asJson)

    actual match {
      case Left(t)  => assert(t.getMessage.contains("{bad-json}"))
      case Right(_) => fail("expected to fail but didn't")
    }
  }
}
