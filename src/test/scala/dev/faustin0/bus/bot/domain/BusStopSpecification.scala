package dev.faustin0.bus.bot.domain

import fastparse.Parsed
import org.scalacheck._

import java.util.Calendar.{ HOUR_OF_DAY, MINUTE }

object BusStopSpecification extends Properties("BusStopQuery") {

  import org.scalacheck.Prop.{ forAll => forAllCheck }

  private val spacesGen    = Gen.nonEmptyListOf(Gen.const(" ")).map(_.mkString)
  private val endSpacesGen = Gen.listOf(Gen.const(" ")).map(_.mkString)

  private val stop = (for {
    stop        <- Gen.posNum[Int]
    maybeSpaces <- endSpacesGen
  } yield s"$stop$maybeSpaces").suchThat(_.nonEmpty)

  private val stopAndBus = for {
    stop   <- stop
    spaces <- spacesGen
    bus    <- Gen.alphaNumStr.suchThat(_.nonEmpty)
  } yield s"$stop$spaces$bus"

  private val stopAndBusAndHour = for {
    stopAndBus   <- stopAndBus
    spaces       <- spacesGen
    calendar     <- Gen.calendar
    hourAndMinute = s"${calendar.get(HOUR_OF_DAY)}:${calendar.get(MINUTE)}"
  } yield s"$stopAndBus$spaces$hourAndMinute"

  property("stop") = forAllCheck(stop) { q: String =>
    val query = BusInfoQuery.fromText(q)
    query match {
      case NextBusQuery(stop, None, None) => true
      case _                              => false
    }
  }

  property("stop and bus") = forAllCheck(stopAndBus) { q: String =>
    val query = BusInfoQuery.fromText(q)
    query match {
      case NextBusQuery(stop, Some(value), None) => value.nonEmpty
      case _                                     => false
    }
  }

  property("stop and bus and hour") = forAllCheck(stopAndBusAndHour) { q: String =>
    val query = BusInfoQuery.fromText(q)
    query match {
      case NextBusQuery(stop, Some(bus), Some(_)) => bus.nonEmpty
      case _                                      => false
    }
  }

  property("stop and bus and hour fastparse") = forAllCheck(stopAndBusAndHour) { q: String =>
    val query = fastparse.parse(q, BusInfoQuery.parseNextBusQuery(_), true)
    println(s"$q  =>  $query")
    query match {
      case Parsed.Success(NextBusQuery(_, Some(bus), Some(_)), _) => bus.nonEmpty
      case _                                                      => false
    }
  }

  val bus = spacesGen.map(_.mkString).flatMap(s => Gen.alphaNumStr.suchThat(_.nonEmpty).map(s + _))

  property("bus fastparse") = forAllCheck(bus) { q: String =>
    val query = fastparse.parse(q, BusInfoQuery.busStopNameParser(_), true)
    query match {
      case Parsed.Success(value, _) => value.nonEmpty && !value.startsWith(" ")
      case _: Parsed.Failure        => false
    }
  }

}
