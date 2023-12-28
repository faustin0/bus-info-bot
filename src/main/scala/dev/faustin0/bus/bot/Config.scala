package dev.faustin0.bus.bot

import cats.effect.IO
import cats.effect.std.Env
import cats.syntax.all._
import org.http4s.Uri

case class Config(
  telegramToken: String,
  busAppUri: Uri
)

object Config {

  def load: IO[Config] =
    (
      Env[IO].get("TOKEN").flatMap(_.liftTo[IO](new NoSuchElementException("Missing token env"))),
      Uri.fromString("https://bus-app.fware.net/").liftTo[IO]
    )
      .mapN(Config.apply)

}
