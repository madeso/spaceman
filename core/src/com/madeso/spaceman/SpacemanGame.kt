package com.madeso.spaceman

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable

import com.madeso.engine.*

class Assets : Disposable {
  var dest = Destructor()

  var blue_grass = dest.ret(Texture(Gdx.files.internal("blue_grass.png")))
  var pack = dest.ret(TextureAtlas(Gdx.files.internal("pack.atlas")))

  override fun dispose() {
    dest.dispose()
  }
}

class Alien(assets:Assets, private val startX : Float, private val startY : Float) : ObjectController {
  override fun act(delta: Float, remove: ObjectRemote) {
  }

  override fun dispose() {
  }

  override fun init(remote: ObjectRemote) {
    remote.setRenderSize(70f, 70f * 2)
    remote.teleport(startX, startY)
    // setPosition(x, y, Align.bottomLeft)
  }

  val stand = Animation(1.0f, assets.pack.newSprite("player/alienGreen_stand"))
}

class SpacemanGame : Game() {
  lateinit private var assets : Assets
  private val worldCreators = CreatorList()

  override fun create() {
    assets = Assets()

    worldCreators.registerCreator("alien-body", object: ObjectCreator {
      override fun create(map: ObjectCreatorDispatcher, x: Float, y: Float, tile: TiledMapTileMapObject) {
        val alien = Alien(assets, x, y)
        map.addObject(alien.stand, alien)
      }
    })
    worldCreators.registerNullCreator("alien-head")

    loadWorld("test.tmx")
  }

  fun loadWorld(path:String) {
    val self = this
    setScreen(BasicLoaderScreen("test.tmx", worldCreators, object:Loader {
      override fun worldLoaded(map: World) {
        var background = ImageActor(assets.blue_grass)
        background.setSize(HEIGHT, HEIGHT)
        background.setPosition(0f, 0f, Align.bottomLeft)
        map.renderWorld.back.addActor(background)

        background = ImageActor(assets.blue_grass)
        background.setSize(HEIGHT, HEIGHT)
        background.setPosition(HEIGHT, 0f, Align.bottomLeft)
        map.renderWorld.back.addActor(background)

        self.setScreen(WorldScreen(map))
      }

    }))
  }

  override fun dispose() {
    assets.dispose()
  }
}
