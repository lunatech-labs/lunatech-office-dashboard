package models

import akka.actor._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.actor._
import scala.concurrent.duration._

import akka.pattern.ask

import play.api.Play.current
import play.libs.F.Promise
import akka.util.Timeout

import play.api.libs.concurrent.Execution.Implicits._

object Office {
    
  var checkedIn = Map.empty[String, String]
  var motdMessage = "none"
  var vacationMessage = "no one"


  implicit val timeout = Timeout(1 second)


  lazy val default = {
    val roomActor = Akka.system.actorOf(Props[Office])

    roomActor
  }

  def join(username:String) = {
      default ! Join(username)
  }
  
  def quit(username:String) = {
      default ! Quit(username)
  }

  def motd(username:String, message:String) = {
      default ! Motd(username, message)
  }
  
  def vacation(username:String, message:String) = {
        default ! Vacation(username, message)
  }

    def listen(): scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {
    (default ? Listen()).map {

      case Connected(enumerator) =>
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
            (event \ "kind").as[String] match {
                case "vacation" => default ! Vacation((event \ "user").as[String], (event \ "message").as[String])
                case "motd" => default ! Motd((event \ "user").as[String], (event \ "message").as[String])
                case _ => default ! Listen()
            }

        }

        (iteratee, enumerator)

      case CannotConnect(error) =>
        // Connection error

        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue,Unit]((),Input.EOF)

        // Send an error and close the socket
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)

    }

  }
  
}

class Office extends Actor {

 // Message: user: user, action: join  
  var channels = List.empty[PushEnumerator[JsValue]]
  
  def receive = {
    case Listen() => {
      val channel =  Enumerator.imperative[JsValue]()
      channels = channel +: channels
      sender ! Connected(channel)
    }
    
    case Join(username) => {
      Office.checkedIn = Office.checkedIn + (username -> username)
      notifyAll("join", username, "checkin in the office")
    }
  
    case Quit(username) => {
      Office.checkedIn = Office.checkedIn - username
      notifyAll("quit", username, "checkout out of the office")
    }
    
    case Motd(username, message) => {
      Office.motdMessage = message
      notifyAll("motd", username, message)
    }
    

  }
  
  def notifyAll(kind: String, user: String, text: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> JsString(text),
        "members" -> JsArray(
          Office.checkedIn.keySet.toList.map(JsString)
        )
      )
    )
    channels.foreach { 
       _.push(msg)
    }
  }
  
}

case class Listen()

case class Join(username: String)
case class Quit(username: String)
case class Motd(username: String, message: String)
case class Vacation(username: String, message: String)

case class Connected(enumerator:Enumerator[JsValue])
case class CannotConnect(msg: String)
