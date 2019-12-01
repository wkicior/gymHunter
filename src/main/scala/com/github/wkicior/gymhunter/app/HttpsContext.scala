package com.github.wkicior.gymhunter.app

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

case class HttpsContext(settings: SettingsImpl) {

  def isAvailable: Boolean = {
    !settings.gymhunterKeystorePassword.isEmpty && getClass.getClassLoader.getResource(settings.gymhunterKeystorePath) != null
  }

  def apply(): HttpsConnectionContext = {
    val password: Array[Char] = settings.gymhunterKeystorePassword.toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream(settings.gymhunterKeystorePath)

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    ConnectionContext.https(sslContext)
  }
}
