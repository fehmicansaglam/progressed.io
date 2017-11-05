package io.progressed

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

object Boot extends App {

  implicit val system: ActorSystem = ActorSystem("progressed-io-system")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  // create and start our service actor
  val service = new ProgressedIOService

  Http().bindAndHandle(service.routes, "0.0.0.0", 8080)

}
