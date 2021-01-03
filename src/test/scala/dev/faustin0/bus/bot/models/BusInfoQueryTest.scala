package dev.faustin0.bus.bot.models

import java.time.LocalTime

import org.scalatest.Inside
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

class BusInfoQueryTest extends AnyFunSuite with Inside with Matchers {

  val fullQueries =
    Table(
      "text", // First tuple defines column names
      "303 24 1430",
      "303 24 14:30",
      "303   24    1430"
    )

  forAll(fullQueries) { (text: String) =>
    test(s"should extract a full query from a text message $text") {
      val expected = NextBus("303", Some("24"), Some(LocalTime.of(14, 30)))

      val query = BusInfoQuery.fromText(text)

      assert(query === expected)

    }
  }

  val queryWithBus =
    Table(
      "text",
      "303 24",
      "303  24",
      "303 14a",
      "303   14C"
    )

  forAll(queryWithBus) { (text: String) =>
    test(s"should extract a partial query from a text message $text") {

      val query = BusInfoQuery.fromText(text)

      inside(query) { case NextBus(_, Some(_), None) => succeed }
    }
  }

  test("should extract a partial query from a text message with missing bus and hour") {
    val textMsg  = "303"
    val expected = NextBus("303", None, None)

    val query = BusInfoQuery.fromText(textMsg)

    assert(query === expected)

  }

  test("should extract a bus stop 'search by name' query") {
    val textMsg  = "bus stop name"
    val expected = BusStopInfo(textMsg)

    val query = BusInfoQuery.fromText(textMsg)

    assert(query === expected)

  }
}
