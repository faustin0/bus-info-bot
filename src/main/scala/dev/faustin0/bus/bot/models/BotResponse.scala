package dev.faustin0.bus.bot.models

case class BotResponse[Keyboard](
  message: String,
  keyboard: Option[Keyboard]
)
