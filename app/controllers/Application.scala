package controllers


import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.api.libs.iteratee._

import play.api.libs.concurrent.Execution.Implicits._

import models._


object Application extends Controller {
  
  def index = Action {  implicit request =>
    Ok(views.html.index("", Office.checkedIn, Office.motdMessage, Office.vacationMessage))
  }
  
  def checkin(email:String) = Action {
     Office.join(email)
     Ok("")
   }
   
  def checkout(email:String) = Action {
     Office.quit(email)
     Ok("")
  }

  def listen() = WebSocket.async[JsValue] { request  =>
    Office.listen()
  }
  
}