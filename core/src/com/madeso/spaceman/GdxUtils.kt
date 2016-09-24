package com.madeso.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
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

class Layers(val batch : Batch) {
  var camera = OrthographicCamera()
  var viewport = StretchViewport(WIDTH, HEIGHT, camera);
  var stages = Array<Stage>()

  fun newStage(): Stage {
    val stage = Stage(viewport, batch)
    stages.add(stage)
    return stage
  }

  fun act(delta: Float) {
    for(stage in stages) {
      stage.act(delta)
    }
  }

  fun apply() {
    viewport.apply()
    camera.update();
    batch.projectionMatrix = camera.combined;
  }

  fun render() {
    apply()

    for( stage in stages) {
      stage.draw()
    }
  }

  fun resize(width: Int, height: Int) {
    viewport.update(width, height)
  }
}

fun TextureAtlas.newSprite(path : String): TextureAtlas.AtlasRegion {
  return this.findRegion(path) ?: throw Exception("Unable to load " + path)
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

