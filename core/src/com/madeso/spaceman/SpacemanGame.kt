package com.madeso.spaceman

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.StretchViewport

val WIDTH: Float = 2560f;
val HEIGHT: Float = 1440f;

class Assets : Disposable {
  var mgr = SmartAssetManager()

  override fun dispose() {
    mgr.dispose()
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
  internal var dest = Destructor()
  internal var map : OrthoMap = assets.mgr.orthoMap(dest, "test.tmx")

  init {
    map.registerNullCreator("alien-body")
    map.registerNullCreator("alien-head")
  }

  override fun render(delta: Float) {
    d.act(delta)
    ClearScreen()
    map.render(d.camera)
    d.render()
    d.uirender()
  }

  override fun resize(width: Int, height: Int) {
    d.resize(width, height)
  }
}

class LoaderScreen(private val game : Game, private val assetManager: SmartAssetManager, private val gs: GameScreen) : ScreenAdapter() {
  private var loaded = false

  init {
  }

  override fun render(delta: Float) {
    ClearScreen()

    // this.camera.update()
    // this.game.batch.setProjectionMatrix(camera.combined)

    if (this.loaded) {
      /*game.batch.begin()
      game.font.draw(game.batch, " ", 100, 150)
      game.font.draw(game.batch, "Loading done. Touch to play.", 100, 100)
      game.batch.end()*/

      if (true) { // Gdx.input.isTouched()) {
        assetManager.finishLoading()
        assetManager.postLoad()
        game.screen = this.gs
        this.dispose()
      }
    } else {
      if (assetManager.update()) {
        assetManager.finishLoading()
        assetManager.postLoad()

        if (assetManager.isFinished) {
          this.loaded = true
        }
      }

      // display loading information
      val progress = assetManager.progress

      /*game.batch.begin()
      game.font.draw(game.batch, "Please wait...", 100, 150)
      game.font.draw(game.batch, "Loading: " + java.lang.Float.toString(progress * 100) + "%", 100, 100)
      game.batch.end()*/
    }
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
    setScreen(LoaderScreen(this, assets.mgr, GameScreen(assets, batch)))
  }

  override fun dispose() {
    batch.dispose()
    assets.dispose()
  }
}
