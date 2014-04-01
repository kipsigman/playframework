/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.filters.csrf

import play.api.libs.ws.WS.WSRequestHolder
import scala.concurrent.Future
import play.api.libs.ws.{WS, Response}
import play.api.mvc._

/**
 * Specs for the Scala per action CSRF actions
 */
object ScalaCSRFActionSpec extends CSRFCommonSpecs {

  def buildCsrfCheckRequest(sendUnauthorizedResult: Boolean, configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[Response])(handleResponse: (Response) => T) = withServer(configuration) {
      case _ => if (sendUnauthorizedResult) {
        CSRFCheck(Action(Results.Ok), new CustomErrorHandler())
      } else {
        CSRFCheck(Action(Results.Ok))
      }
    } {
      import play.api.Play.current
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }

  def buildCsrfAddToken(configuration: (String, String)*) = new CsrfTester {
    def apply[T](makeRequest: (WSRequestHolder) => Future[Response])(handleResponse: (Response) => T) = withServer(configuration) {
      case _ => CSRFAddToken(Action {
        implicit req =>
          CSRF.getToken(req).map {
            token =>
              Results.Ok(token.value)
          } getOrElse Results.NotFound
      })
    } {
      import play.api.Play.current
      handleResponse(await(makeRequest(WS.url("http://localhost:" + testServerPort))))
    }
  }

  class CustomErrorHandler extends CSRF.ErrorHandler {
    import play.api.mvc.Results.Unauthorized
    def handle(msg: String) = Unauthorized(msg)
  }
}
