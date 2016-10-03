package com.madeso.engine

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.StretchViewport

val WIDTH: Float = 2560f;
val HEIGHT: Float = 1440f;

fun ClearScreen() {
  Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
  Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
}

fun TextureAtlas.newSprite(path : String): TextureAtlas.AtlasRegion {
  return this.findRegion(path) ?: throw Exception("Unable to load " + path)
}

fun Animation.setLooping() : Animation {
  this.playMode = Animation.PlayMode.LOOP
  return this
}

class ImageActor(var img: Texture) : Actor() {
  init {
    width = img.width.toFloat()
    height = img.height.toFloat()
    setOrigin(Align.topLeft)
  }

  override fun draw(batch: Batch?, parentAlpha: Float) {
    batch?.draw(img, x, y, width, height)
  }
}

abstract class SuperGame(private var game: Game) {
  fun setScreen(screen : Screen) {
    game.screen = screen;
  }
}
