package com.madeso.spaceman.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.madeso.spaceman.HEIGHT
import com.madeso.spaceman.SpacemanGame
import com.madeso.spaceman.WIDTH

object DesktopLauncher {
    @JvmStatic fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        val scale = 0.35f;
        config.width = (WIDTH * scale).toInt();
        config.height = (HEIGHT * scale).toInt();
        LwjglApplication(SpacemanGame(), config)
    }
}
