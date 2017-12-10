package io.progressed

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.CachingDirectives._
import akka.http.scaladsl.server.{Directive0, RequestContext, Route}
import akka.util.ByteString
import com.codahale.metrics.json.MetricsModule
import com.fasterxml.jackson.databind.ObjectMapper
import nl.grons.metrics.scala.{DefaultInstrumented, MetricName}

import scala.xml.Elem

case class SvgParams(progress: Int, scale: Int, title: Option[String], suffix: String) {
  require(progress >= 0 && progress <= 999, "progress must be [0-999]")
  require(suffix.length == 1, "suffix size must be 1")
}

class ProgressedIOService(implicit system: ActorSystem) extends DefaultInstrumented {

  override lazy val metricBaseName = MetricName("")

  private[this] val mapper = (new ObjectMapper)
    .registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false))

  private def getSvg(svgParams: SvgParams): Elem = {
    import svgParams._

    val titleWidth = title.map(t => s"$t".length * 6 + 10).getOrElse(0)
    val progressWidth = if (title.isDefined) 60.0 else 90.0
    val totalWidth = titleWidth + progressWidth
    val width = progressWidth * progress / scale
    val progressX = titleWidth + (progressWidth / 2)
    val color: String = progress.toDouble / scale.toDouble match {
      case p if p < 0.3 => "#d9534f"
      case p if p < 0.7 => "#f0ad4e"
      case _ => "#5cb85c"
    }

    <svg xmlns="http://www.w3.org/2000/svg" width={s"$totalWidth"} height="20">
      <linearGradient id="a" x2="0" y2="100%">
        <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
        <stop offset="1" stop-opacity=".1"/>
      </linearGradient>
      <rect rx="4" x="0" width={s"$totalWidth"} height="20" fill="#428bca"/>
      <rect rx="4" x={s"$titleWidth"} width={s"$progressWidth"} height="20" fill="#555"/>
      <rect rx="4" x={s"$titleWidth"} width={s"$width"} height="20" fill={color}/>{if (title.isDefined) {
        <path fill={color} d={s"M$titleWidth 0h4v20h-4z"}/>
    }}<rect rx="4" width={s"$totalWidth"} height="20" fill="url(#a)"/>
      <g fill="#fff" text-anchor="left" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="11">
        <text x="4" y="15" fill="#010101" fill-opacity=".3">
          {title.getOrElse("")}
        </text>
        <text x="4" y="14">
          {title.getOrElse("")}
        </text>
      </g>
      <g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="11">
        <text x={s"$progressX"} y="15" fill="#010101" fill-opacity=".3">
          {progress + suffix}
        </text>
        <text x={s"$progressX"} y="14">
          {progress + suffix}
        </text>
      </g>
    </svg>
  }

  val timed: Directive0 = {
    extractRequest.flatMap { implicit request =>
      val path = request.uri.path.toString.drop(1).replaceAll("/", ".")
      val method = request.method.name

      val startTime = System.currentTimeMillis()
      mapResponse { response =>
        val elapsed = System.currentTimeMillis() - startTime

        metrics
          .timer(s"response.${response.status.intValue()}.$method.$path")
          .update(elapsed, TimeUnit.MILLISECONDS)

        response
      }
    }
  }

  val routes: Route = timed {
    get {
      (path("bar" / IntNumber) & parameters(('scale.?(100), 'title.?, 'suffix.?("%")))).as(SvgParams) { svgParams =>
        encodeResponse {
          alwaysCache(routeCache, keyer = {
            case r: RequestContext => r.request.uri
          }) {
            complete {
              HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`image/svg+xml`), ByteString(getSvg(svgParams).toString())))
            }
          }
        }
      } ~ path("metrics") {
        complete {
          mapper.writeValueAsString(metricRegistry)
        }
      } ~
        path("ping") {
          complete {
            "pong"
          }
        }
    }
  }

}
