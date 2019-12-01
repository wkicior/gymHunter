package com.github.wkicior.gymhunter.app

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config.Config

class SettingsImpl(config: Config) extends Extension {
  val gymsteerHost: String = config.getString("gymhunter.gymsteer.host")
  val ifttHost: String = config.getString("gymhunter.iftt.host")
  val ifttKey: String = config.getString("gymhunter.iftt.key")
  val basicAuthPassword: String = config.getString("gymhunter.auth.password")
  val gymsteerUsername: String = config.getString("gymhunter.gymsteer.username")
  val gymsteerPassword: String = config.getString("gymhunter.gymsteer.password")
  val gymhunterKeystorePath: String = config.getString("gymhunter.keystore.path")
  val gymhunterKeystorePassword: String = config.getString("gymhunter.keystore.password")
}
object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new SettingsImpl(system.settings.config)

  /**
    * Java API: retrieve the Settings extension for the given system.
    */
  override def get(system: ActorSystem): SettingsImpl = super.get(system)
}