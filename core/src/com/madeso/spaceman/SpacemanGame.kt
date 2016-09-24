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
import com.badlogic.gdx.utils.Array
import javax.swing.GroupLayout

val WIDTH: Float = 2560f;
val HEIGHT: Float = 1440f;

class Assets : Disposable {
  var mgr = SmartAssetManager()
  var blue_grass = Texture(Gdx.files.internal("blue_grass.png"))

  override fun dispose() {
    mgr.dispose()
  }
}

class Layers(var batch : SpriteBatch) {
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
    apply();
    for( stage in stages) {
      stage.draw()
    }
  }

  fun resize(width: Int, height: Int) {
    viewport.update(width, height)
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
  internal var layersBackground = Layers(batch)
  internal var backgroundLayer = layersBackground.newStage()

  internal var layersWorld = Layers(batch)
  internal var foreground = layersWorld.newStage()

  internal var dest = Destructor()
  internal var map : OrthoMap = assets.mgr.orthoMap(dest, "test.tmx")

  init {
    var background = ImageActor(assets.blue_grass)
    background.setSize(HEIGHT, HEIGHT)
    background.setPosition(0f, 0f, Align.bottomLeft)
    backgroundLayer.addActor(background)

    background = ImageActor(assets.blue_grass)
    background.setSize(HEIGHT, HEIGHT)
    background.setPosition(HEIGHT, 0f, Align.bottomLeft)
    backgroundLayer.addActor(background)

    map.registerNullCreator("alien-body")
    map.registerNullCreator("alien-head")
  }

  override fun render(delta: Float) {
    layersBackground.act(delta)
    layersBackground.act(delta)
    ClearScreen()
    layersBackground.render()
    layersWorld.apply()
    map.render(layersWorld.camera)
    layersWorld.render()
  }

  override fun resize(width: Int, height: Int) {
    layersBackground.resize(width, height)
    layersWorld.resize(width, height)
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
