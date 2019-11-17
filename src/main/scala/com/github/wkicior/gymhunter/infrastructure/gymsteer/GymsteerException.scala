package com.github.wkicior.gymhunter.infrastructure.gymsteer

final case class GymsteerException(msg: String) extends RuntimeException(msg)