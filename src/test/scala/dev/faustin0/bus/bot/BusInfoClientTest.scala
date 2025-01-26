package dev.faustin0.bus.bot

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{ IO, Resource }
import com.dimafeng.testcontainers.{ ForAllTestContainer, MockServerContainer }
import dev.faustin0.bus.bot.domain._
import dev.faustin0.bus.bot.infrastructure.Http4sBusInfoClient
import io.circe.literal.JsonStringContext
import org.http4s.Uri
import org.http4s.client.{ Client, JavaNetClientBuilder }
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.wait.strategy.Wait

import java.time.LocalTime

class BusInfoClientTest extends AsyncFreeSpec with ForAllTestContainer with AsyncIOSpec with Matchers {

  val httpClient: Client[IO] = JavaNetClientBuilder[IO].create

  override val container: MockServerContainer = MockServerContainer("5.13.2").configure { c =>
    c.waitingFor(Wait.forLogMessage(".*started on port:.*", 1))
  }

  lazy val mockServerClient: Resource[IO, MockServerClient] =
    Resource.make(
      IO(new MockServerClient(container.host, container.container.getServerPort))
    )(client => IO(client.reset()))

  "should retrieve the next buses for a given stop" in
    mockServerClient.use { mock =>
      val registerExpectation = IO(
        mock
          .when(
            request()
              .withPath("/bus-stops/303")
              .withMethod("GET")
              .withQueryStringParameter("bus", "28")
          )
          .respond(
            response().withBody(
              json"""
                  [
                    {
                      "busStopCode": 303,
                      "bus": "28",
                      "satellite": true,
                      "hour": "23:00",
                      "busInfo": ""
                    },
                    {
                      "busStopCode": 303,
                      "bus": "28",
                      "satellite": false,
                      "hour": "23:20",
                      "busInfo": ""
                    }
                  ]
                """.noSpaces
            )
          )
      )
      for {
        _        <- registerExpectation
        sut      <- IO(Http4sBusInfoClient(httpClient, Uri.unsafeFromString(container.endpoint)))
        response <- sut.getNextBuses(NextBusQuery("303", Some("28")))
      } yield response
    }.asserting {
      case Right(IncomingBuses(_, _, buses)) => assert(buses.nonEmpty)
      case _                                 => fail()
    }

  "should retrieve the next buses for a given stop and hour" in
    mockServerClient.use { mock =>
      val registerExpectation = IO(
        mock
          .when(
            request()
              .withPath("/bus-stops/303")
              .withMethod("GET")
              .withQueryStringParameter("bus", "28")
              .withQueryStringParameter("hour", "16:30")
          )
          .respond(
            response().withBody(
              json"""
                  [
                    {
                      "busStopCode": 303,
                      "bus": "28",
                      "satellite": true,
                      "hour": "16:37",
                      "busInfo": ""
                    },
                    {
                      "busStopCode": 303,
                      "bus": "28",
                      "satellite": false,
                      "hour": "16:39",
                      "busInfo": ""
                    }
                  ]
                """.noSpaces
            )
          )
      )
      for {
        _        <- registerExpectation
        sut      <- IO(Http4sBusInfoClient(httpClient, Uri.unsafeFromString(container.endpoint)))
        response <- sut.getNextBuses(NextBusQuery("303", Some("28"), Some(LocalTime.of(16, 30))))
      } yield response
    }.asserting {
      case Right(IncomingBuses(_, _, buses)) => assert(buses.nonEmpty)
      case _                                 => fail()
    }

  "should get a MissingBusStop when bus stop does not exist" in
    mockServerClient.use { mock =>
      val registerExpectation = IO(
        mock
          .when(
            request()
              .withPath("/bus-stops/2023")
              .withMethod("GET")
          )
          .respond(
            response()
              .withStatusCode(404)
              .withBody(
                json"""
                       {
                       "msg": "2023 not handled"
                       }
                  """.noSpaces
              )
          )
      )
      for {
        _        <- registerExpectation
        sut      <- IO(Http4sBusInfoClient(httpClient, Uri.unsafeFromString(container.endpoint)))
        response <- sut.getNextBuses(NextBusQuery("2023"))
      } yield response
    }.asserting {
      case Left(MissingBusStop()) => succeed
      case _                      => fail()
    }

  "should get a BadRequest on malformed request" in
    mockServerClient.use { mock =>
      val registerExpectation = IO(
        mock
          .when(
            request()
              .withPath("/bus-stops/2022")
              .withMethod("GET")
              .withQueryStringParameter("bus", "wrong")
          )
          .respond(
            response()
              .withStatusCode(400)
              .withBody(
                json"""
                       {
                         "msg": "bus not handled"
                       }
                  """.noSpaces
              )
          )
      )
      for {
        _        <- registerExpectation
        sut      <- IO(Http4sBusInfoClient(httpClient, Uri.unsafeFromString(container.endpoint)))
        response <- sut.getNextBuses(NextBusQuery("2022", Some("wrong")))
      } yield response
    }.asserting {
      case Left(BadRequest()) => succeed
      case _                  => fail()
    }

  "should get an empty NextBusResponse when no more bus for a given stop" in
    mockServerClient.use { mock =>
      val registerExpectation = IO(
        mock
          .when(
            request()
              .withPath("/bus-stops/303")
              .withMethod("GET")
              .withQueryStringParameter("bus", "85")
          )
          .respond(
            response()
              .withStatusCode(200)
              .withBody(json"[]".noSpaces)
          )
      )
      for {
        _        <- registerExpectation
        sut      <- IO(Http4sBusInfoClient(httpClient, Uri.unsafeFromString(container.endpoint)))
        response <- sut.getNextBuses(NextBusQuery("303", Some("85")))
      } yield response
    }.asserting(resp => resp shouldBe Right(NoMoreBus(BusStop(303), Some(Bus("85")))))

  "should search a bus stop by name" in
    mockServerClient.use { mock =>
      val registerExpectation = IO(
        mock
          .when(
            request()
              .withPath("/bus-stops")
              .withQueryStringParameter("name", "stazione centrale")
              .withMethod("GET")
          )
          .respond(
            response()
              .withStatusCode(200)
              .withBody(
                json"""
                    [
                      {
                        "code": 7,
                        "name": "STAZIONE CENTRALE",
                        "location": "PIAZZA MEDAGLIE D`ORO (PENSILINA D)",
                        "comune": "BOLOGNA",
                        "areaCode": 500,
                        "position": {
                          "x": 686322,
                          "y": 930912,
                          "lat": 44.505714,
                          "long": 11.342895
                          }
                        },
                        {
                          "code": 471,
                          "name": "STAZIONE CENTRALE",
                          "location": "VIALE PIETRAMELLARA (PENSILINA L)",
                          "comune": "BOLOGNA",
                          "areaCode": 500,
                          "position": {
                            "x": 686462,
                            "y": 930813,
                            "lat": 44.504787,
                            "long": 11.34462
                            }
                          }
                      ]
                  """.noSpaces
              )
          )
      )
      for {
        _        <- registerExpectation
        sut      <- IO(Http4sBusInfoClient(httpClient, Uri.unsafeFromString(container.endpoint)))
        response <- sut.searchBusStopByName(BusStopInfo("stazione centrale"))
      } yield response
    }.asserting {
      case Right(BusStopDetailsResponse(::(head, _))) => assert(head.name == "STAZIONE CENTRALE")
      case Right(BusStopDetailsResponse(Nil))         => fail("expected a list of bus stop")
      case _                                          => fail()
    }

}
