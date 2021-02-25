package dev.faustin0.bus.bot.infrastructure

import cats.effect.{ ContextShift, IO, Resource }
import dev.faustin0.bus.bot.domain.{ User, UserRepository }
import dev.faustin0.bus.bot.infrastructure.DynamoUserRepository.{ JavaFutureOps, Table }
import io.chrisdavenport.log4cats.Logger
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.model.{ AttributeValue, GetItemRequest, PutItemRequest }

import java.util.concurrent.{ CancellationException, CompletableFuture }
import java.util.{ Map => JavaMap }
import scala.jdk.CollectionConverters.MapHasAsScala

class DynamoUserRepository private (private val client: DynamoDbAsyncClient)(implicit
  log: Logger[IO],
  cs: ContextShift[IO]
) extends UserRepository[IO] {

  override def create(user: User): IO[Unit] = {
    val request = PutItemRequest
      .builder()
      .tableName(Table.name)
      .item(
        JavaMap.of(
          Table.id,
          attribute(_.n(String.valueOf(user.id))),
          Table.firstName,
          attribute(_.s(user.firstName)),
          Table.lastName,
          attribute(_.s(user.lastName)),
          Table.userName,
          attribute(_.s(user.userName)),
          Table.language,
          attribute(_.s(user.language.getOrElse("")))
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
      _      <- log.info(s"Getting user $id")
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

  private def attribute(b: AttributeValue.Builder => Unit): AttributeValue = {
    val builder = AttributeValue.builder()
    b(builder)
    builder.build()
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

    def fromCompletable: IO[T] =
      unevaluatedCF.flatMap { cf =>
        IO.cancelable { callback =>
          cf.handle((res: T, err: Throwable) =>
            err match {
              case null                     => callback(Right(res))
              case _: CancellationException => ()
              case ex                       => callback(Left(ex))
            }
          )
          IO.delay(cf.cancel(true))
        }
      }
  }

  def apply(dynamoClient: DynamoDbAsyncClient)(implicit cs: ContextShift[IO], l: Logger[IO]): UserRepository[IO] =
    new DynamoUserRepository(dynamoClient)

  def makeFromAws(implicit cs: ContextShift[IO], l: Logger[IO]): Resource[IO, UserRepository[IO]] =
    createDynamoRepo {
      DynamoDbAsyncClient
        .builder()
        .build()
    }

  def makeFromEnv(implicit cs: ContextShift[IO], l: Logger[IO]): Resource[IO, UserRepository[IO]] =
    createDynamoRepo {
      DynamoDbAsyncClient
        .builder()
        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
        .region(Region.EU_CENTRAL_1)
        .build()
    }

  private def createDynamoRepo(client: => DynamoDbAsyncClient)(implicit cs: ContextShift[IO], l: Logger[IO]) =
    Resource
      .fromAutoCloseable(IO(client))
      .map(DynamoUserRepository(_))
}
