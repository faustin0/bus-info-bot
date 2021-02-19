package dev.faustin0.bus.bot.infrastructure

import cats.effect.{ IO, Resource }
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.implicits.toTraverseOps
import com.amazonaws.auth.{ AWSStaticCredentialsProvider, BasicAWSCredentials }
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.model.{
  AttributeDefinition,
  AttributeValue,
  KeySchemaElement,
  KeyType,
  ProvisionedThroughput,
  ScalarAttributeType
}
import com.dimafeng.testcontainers.{ Container, ForAllTestContainer, GenericContainer }
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.wait.strategy.Wait

import java.util.{ List => JavaList }
import scala.jdk.CollectionConverters._

class DynamoUserRepositoryTest extends AsyncFreeSpec with ForAllTestContainer with AsyncIOSpec with Matchers {

  private lazy val dynamoContainer = GenericContainer(
    dockerImage = "amazon/dynamodb-local",
    exposedPorts = Seq(8000),
    command = Seq("-jar", "DynamoDBLocal.jar", "-sharedDb", "-inMemory"),
    waitStrategy = Wait.forLogMessage(".*CorsParams:.*", 1)
  ).configure { provider =>
    provider.withLogConsumer((t: OutputFrame) => print(t.getUtf8String))
    ()
  }

  override def container: Container = dynamoContainer

  override def afterStart(): Unit =
    createDynamoClient().use { client =>
      val createTable = IO(
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

      def addItem(id: Int) = IO(
        client.putItem(
          "TelegramUsers",
          Map(
            "id"         -> new AttributeValue().withN(String.valueOf(id)),
            "first_name" -> new AttributeValue().withS(s"aName-$id"),
            "last_name"  -> new AttributeValue().withS(s"lastName-$id"),
            "username"   -> new AttributeValue().withS(s"aUsername-$id")
          ).asJava
        )
      )

      for {
        _ <- createTable
        _ <- (1 to 100).map(addItem).toList.sequence
      } yield ()
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

  "retrieve user by telegram id" in {
    val computation = createDynamoClient().use { client =>
      val sut  = new DynamoUserRepository(client)
      val user = sut.get(1)
      user
    }

    computation.asserting(maybeUser => assert(maybeUser.isDefined))
  }
}
