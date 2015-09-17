package controllers

import play.api._
import play.api.mvc._
import play.api.cache.Cache
import play.api.Play.current
import javax.inject.Inject
import scala.concurrent.Future
import play.api.libs.ws._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class Application @Inject() (ws: WSClient) extends Controller {

  def index = Action {
    Ok("test")
  }
  
  def cors(user:String) = Action.async { request =>
    request.queryString.get("password") match {
      case Some(Seq(password)) => {
        val url = s"https://$user.cloudant.com/_api/v2/user/config/cors"
        val baseRequest = ws
          .url(url)
          .withRequestTimeout(10000)
          .withAuth(user, password, WSAuthScheme.BASIC)
        for {
          domains <- baseRequest.get().map { response =>
            (response.json \ "origins").as[Seq[String]]  
          }
          requestBody = Json.obj(
            "enable_cors" -> true,
            "allow_credentials" -> true,
            "origins" -> (domains ++ Seq("http://cors.test:4567", "http://cors.test:9000")).toSet[String].toSeq
          )
          updateResponse <- baseRequest
            .withHeaders("Content-Type" -> "application/json")
            .put(requestBody)
        
        } yield {
          Ok(updateResponse.body)
        }
          
      }
      case _ => Future(BadRequest("no password supplied\n"))
    }
  }
}
