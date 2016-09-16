package com.madeso.spaceman

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.StretchViewport

val WIDTH: Float = 2560f;
val HEIGHT: Float = 1440f;

class Assets : Disposable {
  var img = Texture("badlogic.jpg")

  override fun dispose() {
    img.dispose()
  }
}

class SharedRendering(var batch : SpriteBatch) {
  var camera = OrthographicCamera()
  var viewport = StretchViewport(WIDTH, HEIGHT, camera);
  var stage = Stage(viewport, batch)

  var uicamera = OrthographicCamera()
  var uiviewport = StretchViewport(WIDTH, HEIGHT, uicamera);
  var uistage = Stage(uiviewport, batch)

  fun act(delta: Float) {
    stage.act(delta)
    uistage.act(delta)
  }

  fun render() {
    camera.update();
    batch.projectionMatrix = camera.combined;
    stage.draw()
  }

  fun uirender() {
    uicamera.update()
    batch.projectionMatrix = uicamera.combined;
    uistage.draw()
  }

  fun resize(width: Int, height: Int) {
    viewport.update(width, height)
    uiviewport.update(width, height)
  }
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

class GameScreen(assets:Assets, var batch : SpriteBatch) : ScreenAdapter() {
  internal var d = SharedRendering(batch)

  init {
    var i = ImageActor(assets.img)
    i.setPosition(0f, 0f, Align.center)
    d.stage.addActor(i)
  }

  override fun render(delta: Float) {
    d.act(delta)
    ClearScreen()
    d.render()
    d.uirender()
  }

  override fun resize(width: Int, height: Int) {
    d.resize(width, height)
  }
}

fun ClearScreen() {
  Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
  Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
}

class SpacemanGame : Game() {
  internal lateinit var batch: SpriteBatch
  internal lateinit var assets: Assets

  override fun create() {
    batch = SpriteBatch()
    assets = Assets()
    setScreen(GameScreen(assets, batch))
  }

  override fun dispose() {
    batch.dispose()
    assets.dispose()
  }
}