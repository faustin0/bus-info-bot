package dev.faustin0.bus.bot.domain

case class User(
  id: Int,
  firstName: String,
  lastName: String,
  userName: String,
  language: Option[String]
)
