package dev.faustin0.bus.bot

import java.util.concurrent.Executors

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{ Blocker, IO }
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.client.{ Client, JavaNetClientBuilder }
import org.scalatest.funsuite.AsyncFunSuite

class BusInfoClientTest extends AsyncFunSuite with AsyncIOSpec {

  val blockingPool           = Executors.newFixedThreadPool(5)
  val blocker                = Blocker.liftExecutorService(blockingPool)
  val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create
  implicit val logger        = Slf4jLogger.getLogger[IO]

  test("should retrieve the next buses for a given stop") {

//    val sut = new BusInfoClient(httpClient)

    assert(true)
  }
}
