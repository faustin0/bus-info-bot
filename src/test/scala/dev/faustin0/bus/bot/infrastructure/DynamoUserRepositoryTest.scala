package dev.faustin0.bus.bot.infrastructure

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{ IO, Resource }
import com.dimafeng.testcontainers.{ Container, ForAllTestContainer, GenericContainer }
import dev.faustin0.bus.bot.domain.User
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.wait.strategy.Wait
import software.amazon.awssdk.auth.credentials.{ StaticCredentialsProvider, _ }
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest.{ Builder => TableBuilder }
import software.amazon.awssdk.services.dynamodb.model._

import java.net.URI

class DynamoUserRepositoryTest extends AsyncFreeSpec with ForAllTestContainer with AsyncIOSpec with Matchers {
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
          .createTable((builder: TableBuilder) =>
            builder
              .tableName("TelegramUsers")
              .attributeDefinitions(
                AttributeDefinition
                  .builder()
                  .attributeName("id")
                  .attributeType(ScalarAttributeType.N)
                  .build()
              )
              .keySchema(
                KeySchemaElement
                  .builder()
                  .attributeName("id")
                  .keyType(KeyType.HASH)
                  .build()
              )
              .provisionedThroughput(
                ProvisionedThroughput
                  .builder()
                  .readCapacityUnits(2000)
                  .writeCapacityUnits(2000)
                  .build()
              )
              .build()
          )
          .get()
      )
    }.unsafeRunSync()

  private def createDynamoClient() = {
    lazy val dynamoDbEndpoint =
      s"http://${dynamoContainer.container.getHost}:${dynamoContainer.container.getFirstMappedPort}"

    Resource.fromAutoCloseable {
      IO(
        DynamoDbAsyncClient
          .builder()
          .region(Region.EU_CENTRAL_1)
          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy")))
          .endpointOverride(URI.create(dynamoDbEndpoint))
          .build()
      )
    }
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
      .map(DynamoUserRepository(_))
      .use { sut =>
        for {
          _ <- sut.create(userToInsert)
          u <- sut.get(userToInsert.id)
        } yield u
      }
      .asserting(maybeUser => maybeUser should contain(userToInsert))
  }

  "getting a non existing user should return an empty user" in {
    createDynamoClient()
      .map(DynamoUserRepository(_))
      .use(sut => sut.get(-10))
      .asserting(maybeUser => maybeUser shouldBe None)
  }
}
