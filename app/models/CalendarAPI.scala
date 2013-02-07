package models

import play.api.mvc.{AsyncResult}
import play.api.libs.concurrent.{Promise, Akka}
import play.api.libs.ws.{Response, WS}
import play.api.libs.json._
import play.api.http._
import play.api._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat}
import play.api.GlobalSettings


object CalendarAPI extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Requesting events from Google calendar API");
    requestEventsFromCalendar
  }

  object Calendar extends Enumeration {
    type Calendar = Value
    val vacations = Value("lunatech.com_3133363733333432333335@resource.calendar.google.com")
    val wolfert = Value("wolfert.de.kraker@lunatech.com")
  }

  private def listEventsFromCalendarURL(calendarId: String): String = {
    CalendarAPIURL + calendarId + listEvents
  }

  private val CalendarAPIURL = "https://www.googleapis.com/calendar/v3/calendars/"
  private val listEvents = "/events"

  private val access_tokenURL = "https://accounts.google.com/o/oauth2/token"
  private val grantType = "urn:ietf:params:oauth:grant-type:jwt-bearer"


  private val alwaysIncludeEmail = "true"
  private val timeMin = "2012-12-31T13:00:00+01:00"

  def authenticateForCalendarAPI: Promise[AccessToken] = {

    /*
    *  create a JWT and sign the JWT with the private key,
    *  and construct an access token request in the appropriate format.
    *
    *  Your application then sends the token request to the Google OAuth 2.0 Authorization Server.
    *  Assuming the application has access to the requested API,
    *  an access token will be returned.
    *
    *  The application can access the API only after receiving the access token.
    *  When the access token expires, the application repeats the process.
    */

    val request = WS.url(access_tokenURL)
    val authRequest: Promise[Response] = request.post(Map(
      "grant_type" -> Seq(grantType),
      "assertion" -> Seq(JWT.build)
    ))

    val authResponse: Promise[AccessToken] = authRequest.map {
      response =>
        val json = Json.parse(response.body)
        val access_token = (json \ "access_token").asOpt[String]
        val token_type = (json \ "token_type").asOpt[String]
        AccessToken(token_type.get, access_token.get)
    }
    authResponse
  }


  def requestEventsFromCalendar = {
    authenticateForCalendarAPI.flatMap {
      token =>
        val request = WS.url(listEventsFromCalendarURL(Calendar.wolfert.toString))
          .withQueryString(("alwaysIncludeEmail", alwaysIncludeEmail))
          .withQueryString(("timeMin", timeMin))
        .withHeaders(HeaderNames.AUTHORIZATION -> token.authorization)
        request.get().map {
          response =>
            val json = Json.parse(response.body)
            val items = (json \ "items").asOpt[List[JsValue]]
            val events: Option[List[Event]] = items.map {
              _.map {
                item =>
                  val startStr = (item \ "start" \ "date").asOpt[String].getOrElse((item \ "start" \ "dateTime").as[String])
                  val endStr = (item \ "end" \ "date").asOpt[String].getOrElse((item \ "start" \ "dateTime").as[String])
                  val end = DateTime.parse(endStr, DateTimeFormat.forPattern(getFormat(endStr)))
                  val start: DateTime = DateTime.parse(startStr, DateTimeFormat.forPattern(getFormat(endStr)))
                  val email = (item \ "creator" \ "email").asOpt[String].getOrElse("undefined")
                  val details = (item \ "summary").asOpt[String].getOrElse("undefined")
                  Event(start, end, email, details)
              }
            }

            if (events.isDefined)
              events.get.filter(_.isToday).map {
                event =>
                  Office.vacation(event.email, event.details)
              }
            else
              Office.vacation("", "Error retreiving Google Calendar events")
        }
    }
  }


  def getFormat(dateStr: String): String = {
    if (dateStr.contains("T")) {
      return "yyyy-MM-dd'T'HH:mm:ssZ"
    }
    "yyyy-MM-dd"
  }
}

case class AccessToken(token_type: String, access_token: String) {
  def authorization: String = {
    token_type + " " + access_token
  }
}

case class Event(start: DateTime, end: DateTime, email: String, details: String) {
  def isToday: Boolean = {
    start.isBeforeNow && end.isAfterNow
  }
}
