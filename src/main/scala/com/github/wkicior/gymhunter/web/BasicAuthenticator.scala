package com.github.wkicior.gymhunter.web

import akka.http.scaladsl.server.directives.Credentials
import com.github.wkicior.gymhunter.app.SettingsImpl

case class BasicAuthenticator(settings: SettingsImpl) {

  def userPassAuthenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p @ Credentials.Provided(id) if p.verify(settings.basicAuthPassword) => Some(id)
      case _ => None
    }
}
