package dev.faustin0.bus.bot.infrastructure

import cats.effect.IO
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{ AttributeValue, GetItemRequest, PutItemRequest, QueryRequest }
import dev.faustin0.bus.bot.domain.{ User, UserRepository }

import java.util.{ Map => JavaMap }

class DynamoUserRepository(private val client: AmazonDynamoDB) extends UserRepository[IO] {

  override def create(user: User): IO[Unit] = {
    val request = new PutItemRequest()
      .withTableName(Table.name)
      .withItem(
        JavaMap.of(
          Table.id,
          new AttributeValue().withN(String.valueOf(user.id)),
          Table.firstName,
          new AttributeValue().withS(user.firstName),
          Table.lastName,
          new AttributeValue().withS(user.lastName),
          Table.userName,
          new AttributeValue().withS(user.userName),
          Table.language,
          new AttributeValue().withS(user.language.getOrElse(""))
        )
      )

    IO(client.putItem(request))
  }

  override def get(id: Int): IO[Option[User]] = {
    val values = JavaMap.of("id", new AttributeValue().withN(id.toString))

    val request = new GetItemRequest()
      .withTableName(Table.name)
      .withKey(values)

    for {
      result <- IO(client.getItem(request))
      item    = Option(result.getItem)
      user   <- IO(item.map { u =>
                  User(
                    id = u.get(Table.id).getN.toInt,
                    firstName = u.get(Table.firstName).getS,
                    lastName = u.get(Table.lastName).getS,
                    userName = u.get(Table.userName).getS,
                    language = Option(u.get(Table.language).getS)
                  )
                })
    } yield user
  }
}

private case object Table {
  val name      = "TelegramUsers"
  val id        = "id"
  val firstName = "first_name"
  val lastName  = "last_name"
  val userName  = "username"
  val language  = "language"
}
