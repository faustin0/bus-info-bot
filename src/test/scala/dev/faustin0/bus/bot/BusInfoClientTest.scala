package dev.faustin0.bus.bot

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{ Blocker, IO, Resource }
import com.dimafeng.testcontainers.{ ForAllTestContainer, MockServerContainer }
import io.circe.literal.JsonStringContext
import org.http4s.Uri
import org.http4s.client.{ Client, JavaNetClientBuilder }
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.wait.strategy.Wait

import java.util.concurrent.Executors

class BusInfoClientTest extends AsyncFreeSpec with ForAllTestContainer with AsyncIOSpec with Matchers {

  val blockingPool           = Executors.newFixedThreadPool(4)
  val blocker                = Blocker.liftExecutorService(blockingPool)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create

  override val container: MockServerContainer = MockServerContainer("5.11.2").configure { c =>
    c.withLogConsumer(c => println(c.getUtf8String))
      .waitingFor(Wait.forLogMessage(".*started on port:.*", 1))
  }

  lazy val mockServerClient: Resource[IO, MockServerClient] = Resource
    .fromAutoCloseable(
      IO(new MockServerClient(container.host, container.container.getServerPort))
    )

  "should retrieve the next buses for a given stop" in {

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
                      "bus": "28",
                      "satellite": false,
                      "hour": "23:00",
                      "busInfo": ""
                    },
                    {
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
        response <- sut.getNextBuses
      } yield response
    }.asserting(responses => assert(responses.nonEmpty))
  }

}
