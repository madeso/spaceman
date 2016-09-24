package com.madeso.spaceman.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.madeso.spaceman.SpacemanGame

object DesktopLauncher {
  @JvmStatic fun main(arg: Array<String>) {
    val config = LwjglApplicationConfiguration()

    // somewhat small 16:9 resolution
    val WIDTH = 640
    val HEIGHT = 400

    config.width = WIDTH;
    config.height = HEIGHT;
    LwjglApplication(SpacemanGame(), config)
  }
}
