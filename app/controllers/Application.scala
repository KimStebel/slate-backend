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
import play.api.libs.iteratee.Enumerator

class Application @Inject() (ws: WSClient) extends Controller {
  
  private implicit def Response2Result(response: Future[WSResponse]): Future[Result] = {
    response map {
      response =>
        val headers = response.allHeaders map {
          h => (h._1, h._2.head)
        } filterNot { _._1.equalsIgnoreCase("Transfer-Encoding") }
        Result(ResponseHeader(response.status, headers), Enumerator(response.body.getBytes))
    }
  }
  
  def graph = Action.async { request =>
    val body = request.body.asJson.get
    val creds = (Json.parse(System.getenv("VCAP_SERVICES")) \ "GraphDataStore").head \ "credentials"
    val Seq(username, password, apiURL) = Seq("username", "password","apiURL") map { fieldName => (creds \ fieldName).as[String] }
    val body2 = Json.obj(
      "gremlin" -> """
        g.V()
         .hasLabel('person')
         .has('type','Actor')
         .has('name','Kevin Bacon')
         .repeat(__.outE().inV().dedup().simplePath())
         .until(__.hasLabel('person').has('name','Bill Paxton'))
         .limit(12)
         .path()
      """
    )
    ws.url(apiURL + "/gremlin")
      .withRequestTimeout(60000)
      .withAuth(username, password, WSAuthScheme.BASIC)
      .post(body)
    
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
