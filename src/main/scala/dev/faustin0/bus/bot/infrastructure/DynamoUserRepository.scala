package dev.faustin0.bus.bot.infrastructure

import cats.effect.{ Blocker, ContextShift, IO, Resource }
import dev.faustin0.bus.bot.domain.{ User, UserRepository }
import dev.faustin0.bus.bot.infrastructure.DynamoUserRepository.{ JavaFutureOps, Table }
import io.chrisdavenport.log4cats.{ Logger, SelfAwareStructuredLogger }
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.{ DynamoDbAsyncClient, DynamoDbAsyncClientBuilder }
import software.amazon.awssdk.services.dynamodb.model.{ AttributeValue, GetItemRequest, PutItemRequest }

import scala.jdk.FutureConverters._
import java.util.concurrent.CompletableFuture
import java.util.{ Map => JavaMap }
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.Try

class DynamoUserRepository private (private val client: DynamoDbAsyncClient)(implicit
  log: Logger[IO],
  cs: ContextShift[IO]
) extends UserRepository[IO] {

  private lazy val blocker = Blocker[IO]

  override def create(user: User): IO[Unit] = {
    val request = PutItemRequest
      .builder()
      .tableName(Table.name)
      .item(
        JavaMap.of(
          Table.id,
          AttributeValue.builder().n(String.valueOf(user.id)).build(),
          Table.firstName,
          AttributeValue.builder().s(user.firstName).build(),
          Table.lastName,
          AttributeValue.builder().s(user.lastName).build(),
          Table.userName,
          AttributeValue.builder().s(user.userName).build(),
          Table.language,
          AttributeValue.builder().s(user.language.getOrElse("")).build()
        )
      )
      .build()

    log.info(s"Creating user $user") *>
      IO(client.putItem(request)).fromCompletable.void
  }

  override def get(id: Int): IO[Option[User]] = {
    val values = JavaMap.of("id", AttributeValue.builder().n(id.toString).build())

    val request = GetItemRequest
      .builder()
      .tableName(Table.name)
      .key(values)
      .build()

    for {
      _      <- log.debug(s"Getting user $id")
      result <- IO(client.getItem(request)).fromCompletable
      item    = result.item().asScala
      user    = for {
                  id        <- item.get(Table.id).flatMap(_.n().toIntOption)
                  firstName <- item.get(Table.firstName).map(_.s())
                  lastName  <- item.get(Table.lastName).map(_.s())
                  userName  <- item.get(Table.userName).map(_.s())
                  language   = item.get(Table.language).map(_.s())
                } yield User(id = id, firstName = firstName, lastName = lastName, userName = userName, language = language)

    } yield user
  }
}

object DynamoUserRepository {

  private case object Table {
    val name      = "TelegramUsers"
    val id        = "id"
    val firstName = "first_name"
    val lastName  = "last_name"
    val userName  = "username"
    val language  = "language_code"
  }

  implicit class JavaFutureOps[T](val unevaluatedCF: IO[CompletableFuture[T]]) extends AnyVal {

    def fromCompletable(implicit cs: ContextShift[IO]): IO[T] =
      IO.fromFuture(unevaluatedCF.map(_.asScala))
  }

  def apply(dynamoClient: DynamoDbAsyncClient)(implicit cs: ContextShift[IO], l: Logger[IO]): UserRepository[IO] =
    new DynamoUserRepository(dynamoClient)

  def makeFromAws(implicit cs: ContextShift[IO], l: Logger[IO]): Try[UserRepository[IO]] =
    Try(DynamoDbAsyncClient.builder().build()).map(DynamoUserRepository(_))

  def makeFromEnv(implicit cs: ContextShift[IO], l: Logger[IO]): Try[UserRepository[IO]] =
    Try {
      DynamoDbAsyncClient
        .builder()
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .region(Region.EU_CENTRAL_1)
        .build()
    }.map(DynamoUserRepository(_))

}
