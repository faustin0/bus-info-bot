package dev.faustin0.bus.bot

import cats.effect.IO
import cats.syntax.all._
import org.http4s.Uri

case class Config(
  telegramToken: String,
  busAppUri: Uri
)

object Config {

  def load: IO[Config] =
    (
      IO(sys.env("TOKEN")),
      Uri.fromString("http://bus-app.fware.net/").liftTo[IO]
    )
      .mapN(Config.apply)

}
