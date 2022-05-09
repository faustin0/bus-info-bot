package dev.faustin0.bus.bot.domain

import canoe.models.{ User => CanoeUser }

case class User(
  id: Long,
  firstName: String,
  lastName: String,
  userName: String,
  language: Option[String]
)

object User {

  def fromTelegramUser(user: CanoeUser): User =
    User(
      id = user.id,
      firstName = user.firstName,
      lastName = user.lastName.getOrElse(""),
      userName = user.username.getOrElse(""),
      language = user.lastName
    )
}
