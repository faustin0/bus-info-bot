package dev.faustin0.bus.bot.infrastructure

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{ IO, Resource }
import com.amazonaws.auth.{ AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model._
import com.dimafeng.testcontainers.{ Container, ForAllTestContainer, GenericContainer }
import dev.faustin0.bus.bot.domain.User
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.testcontainers.containers.wait.strategy.Wait

import java.util.{ List => JavaList }

class DynamoUserRepositoryTest extends AsyncFreeSpec with ForAllTestContainer with AsyncIOSpec {
  implicit private val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private lazy val dynamoContainer = GenericContainer(
    dockerImage = "amazon/dynamodb-local",
    exposedPorts = Seq(8000),
    command = Seq("-jar", "DynamoDBLocal.jar", "-sharedDb", "-inMemory"),
    waitStrategy = Wait.forLogMessage(".*CorsParams:.*", 1)
  ).configure { provider =>
    provider.withLogConsumer(t => logger.debug(t.getUtf8String).unsafeRunSync())
    ()
  }

  override def container: Container = dynamoContainer

  override def afterStart(): Unit =
    createDynamoClient().use { client =>
      IO(
        client
          .createTable(
            JavaList.of(
              new AttributeDefinition("id", ScalarAttributeType.N)
            ),
            "TelegramUsers",
            JavaList.of(
              new KeySchemaElement("id", KeyType.HASH)
            ),
            new ProvisionedThroughput(5, 5)
          )
      )
    }.unsafeRunSync()

  private def createDynamoClient() = {
    lazy val dynamoDbEndpoint =
      s"http://${dynamoContainer.container.getHost}:${dynamoContainer.container.getFirstMappedPort}"

    Resource.make {
      IO(
        AmazonDynamoDBClientBuilder
          .standard()
          .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy")))
          .withEndpointConfiguration(new EndpointConfiguration(dynamoDbEndpoint, null))
          .build()
      )
    }(db => IO(db.shutdown()))
  }

  "create and retrieve user by telegram id" in {
    val userToInsert = User(
      id = 42,
      firstName = "firstName",
      lastName = "lastName",
      userName = "nickName",
      language = Some("it")
    )

    createDynamoClient()
      .map(new DynamoUserRepository(_))
      .use { sut =>
        for {
          _ <- sut.create(userToInsert)
          u <- sut.get(userToInsert.id)
        } yield u
      }
      .asserting(maybeUser => maybeUser should contain(userToInsert))
  }
}
