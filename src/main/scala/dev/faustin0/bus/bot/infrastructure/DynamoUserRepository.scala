package dev.faustin0.bus.bot.infrastructure

import cats.effect.IO
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{ AttributeValue, GetItemRequest, QueryRequest }
import dev.faustin0.bus.bot.domain.{ User, UserRepository }

import java.util.{ Map => JavaMap }

class DynamoUserRepository(private val client: AmazonDynamoDB) extends UserRepository[IO] {
  override def create(user: User): IO[Unit] = ???

  override def get(id: Int): IO[Option[User]] = {
//    val attributes = JavaMap.of("#userIdKey", "id")
    val values = JavaMap.of("id", new AttributeValue().withN(id.toString))

    val request = new GetItemRequest()
      .withTableName("TelegramUsers")
      .withKey(values)

    for {
      result <- IO(client.getItem(request))
      item    = Option(result.getItem)
      user   <- IO(item.map { u =>
                  User(
                    id = u.get("id").getN.toInt,
                    firstName = u.get("first_name").getS,
                    lastName = u.get("last_name").getS,
                    userName = u.get("username").getS,
                    language = None
                  )
                })
    } yield user
  }
}
