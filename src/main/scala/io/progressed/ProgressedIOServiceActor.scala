package io.progressed

import akka.actor.Actor
import spray.http.MediaTypes._
import spray.http._
import spray.routing._

import scala.concurrent.duration.Duration

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class ProgressedIOServiceActor extends Actor with ProgressedIOService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

// this trait defines our service behavior independently from the service actor
trait ProgressedIOService extends HttpService {

  def getSvg(progress: Int, title: Option[String]) = {
    val titleWidth = title.map(t => s"$t".length * 6 + 10).getOrElse(0)
    val progressWidth = if (title.isDefined) 60.0 else 90.0
    val totalWidth = titleWidth + progressWidth
    val width = progressWidth * progress / 100.0
    val progressX = titleWidth + (progressWidth / 2)
    val color: String = progress match {
      case p if p < 30 => "#d9534f"
      case p if p < 70 => "#f0ad4e"
      case _ => "#5cb85c"
    }

    <svg xmlns="http://www.w3.org/2000/svg" width={ s"$totalWidth" } height="18">
      <linearGradient id="a" x2="0" y2="100%">
        <stop offset="0" stop-color="#fff" stop-opacity=".7"/>
        <stop offset=".1" stop-color="#aaa" stop-opacity=".1"/>
        <stop offset=".9" stop-opacity=".3"/>
        <stop offset="1" stop-opacity=".5"/>
      </linearGradient>
      <rect rx="4" x="0" width={ s"$totalWidth" } height="18" fill="#428bca"/>
      <rect rx="4" x={ s"$titleWidth" } width={ s"$progressWidth" } height="18" fill="#555"/>
      <rect rx="4" x={ s"$titleWidth" } width={ s"$width" } height="18" fill={ color }/>
      {
        if (title.isDefined) {
          <path fill={ color } d={ s"M${titleWidth} 0h4v18h-4z" }/>
        }
      }
      <rect rx="4" width={ s"$totalWidth" } height="18" fill="url(#a)"/>
      <g fill="#fff" text-anchor="left" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="11">
        <text x="4" y="13" fill="#010101" fill-opacity=".3">{ title.getOrElse("") }</text>
        <text x="4" y="12">{ title.getOrElse("") }</text>
      </g>
      <g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="11">
        <text x={ s"$progressX" } y="14" fill="#010101" fill-opacity=".3">{ progress }%</text>
        <text x={ s"$progressX" } y="13">{ progress }%</text>
      </g>
    </svg>
  }

  val myRoute =
    path("bar" / IntNumber) { progress =>
      validate(progress >= 0 && progress <= 100, "progress must be [0-100]") {
        get {
          parameter('title ?) { title =>
            compressResponse() {
              respondWithMediaType(`image/svg+xml`) {
                complete {
                  getSvg(progress, title)
                }
              }
            }
          }
        }
      }
    } ~
      path("ping") {
        get {
          complete {
            "pong"
          }
        }
      }
}
