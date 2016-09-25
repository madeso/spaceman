package com.madeso.spaceman

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
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

class Alien(assets:Assets, private val game: SpacemanSuperGame, private val startX : Float, private val startY : Float) : ObjectController {
  override fun act(delta: Float, remote: ObjectRemote) {
    remote.move( PlusMinus(game.controls.right.isDown, game.controls.left.isDown).toFloat() * 70f, 0f )
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

class GameControls(buttons: ButtonList) : BaseGameControls() {
  val left = buttons.newButton().addKeyboard(Input.Keys.LEFT).addKeyboard(Input.Keys.A)
  val right = buttons.newButton().addKeyboard(Input.Keys.RIGHT).addKeyboard(Input.Keys.D)
  val up = buttons.newButton().addKeyboard(Input.Keys.UP).addKeyboard(Input.Keys.W)
  val down = buttons.newButton().addKeyboard(Input.Keys.DOWN).addKeyboard(Input.Keys.S)
}

class SpacemanWorld(args:WorldArg) : World(args) {

}

class SpacemanSuperGame(game:Game) : SuperGame(game) {
  val controls = GameControls(buttons)
  private val assets = Assets()
  private val worldCreators = CreatorList<SpacemanSuperGame>(this)
      .registerCreator("alien-body", object: ObjectCreator<SpacemanSuperGame> {
        override fun create(game: SpacemanSuperGame, world: World, map: ObjectCreatorDispatcher, x: Float, y: Float, tile: TiledMapTileMapObject) {
          val alien = Alien(assets, game, x, y)
          map.addObject(alien.stand, alien)
        }
      })
      .registerNullCreator("alien-head")

  init {
    loadWorld("test.tmx")
  }

  fun loadWorld(path:String) {
    val self = this
    setScreen(BasicLoaderScreen<SpacemanWorld>("test.tmx", worldCreators,
        object:WorldCreator<SpacemanWorld> {
          override fun createWorld(args: WorldArg): SpacemanWorld {
            return SpacemanWorld(args)
          }
        },
        object:Loader<SpacemanWorld> {
          override fun worldLoaded(map: SpacemanWorld) {
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
        }
      )
    )
  }
}

class SpacemanGame : Game() {
  lateinit var game : SpacemanSuperGame

  override fun create() {
    game = SpacemanSuperGame(this)
  }
}
