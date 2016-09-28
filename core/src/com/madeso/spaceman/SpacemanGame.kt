package com.madeso.spaceman

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.math.MathUtils
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

val MOVE_SPEED = 70f * 6f

val JUMP_SPEED = 70f * 6
val GRAVITY = 70f * 32
val MAX_JUMP_TIME = 0.5f
val GHOST_JUMP = 0.1f
val MOVEMENT_ACCELERATION_GROUND = 0.6f
val MOVEMENT_ACCELERATION_AIR = 0.1f

val MAX_Y_SPEED = JUMP_SPEED * 2

class Alien(assets:Assets, private val world: SpacemanWorld, private val startX : Float, private val startY : Float) : ObjectController {
  private var vy = 0f
  private var jumpTime = 0f
  private var curSpeed = 0f
  override fun act(delta: Float, remote: ObjectRemote) {
    if( remote.lastCollision.down && world.controls.jump.isDown == false) {
      vy = 0f
      jumpTime = 0f
    }

    if( remote.lastCollision.x ) {
      curSpeed = 0f
    }

    if( world.controls.jump.isClicked && jumpTime < GHOST_JUMP ) {
      vy = JUMP_SPEED
    }

    if( vy > 0f && world.controls.jump.isDown && jumpTime < MAX_JUMP_TIME) {
      // val exp = ( MAX_JUMP_TIME - jumpTime ) / MAX_JUMP_TIME
      // vy = JUMP_SPEED * exp*exp
      vy -= GRAVITY * delta * (jumpTime / MAX_JUMP_TIME) * (jumpTime / MAX_JUMP_TIME)
    }
    else {
      vy -= GRAVITY * delta
    }

    if( vy < -MAX_Y_SPEED) vy = -MAX_Y_SPEED

    jumpTime += delta

    val hor_move = PlusMinus(world.controls.right.isDown, world.controls.left.isDown)

    val targetSpeed = hor_move.toFloat() * MOVE_SPEED

    val acc = if (remote.lastCollision.down) {
      MOVEMENT_ACCELERATION_GROUND
    }
    else {
      MOVEMENT_ACCELERATION_AIR
    }

    curSpeed = acc * targetSpeed + (1-acc) * curSpeed;
    if( MathUtils.isZero(curSpeed) ) {
      curSpeed = 0f
    }

    remote.move(curSpeed * delta, vy * delta)

    if( remote.lastCollision.down == false) {
      remote.setAnimation(jump)
    }
    else if (hor_move != 0 && !remote.lastCollision.x) {
      remote.setAnimation(walk)
    }
    else {
      remote.setAnimation(stand)
    }

    if( !remote.lastCollision.x ) {
      if( hor_move == 1 ) {
        remote.facingRight = true
      }
      else if( hor_move == -1){
        remote.facingRight = false
      }
    }

    if( remote.outside.down ) {
      vy = 0f
      remote.teleport(startX, startY)
    }

    remote.debug = remote.outside.up
  }

  override fun dispose() {
  }

  override fun init(remote: ObjectRemote) {
    remote.debug = true
    remote.setRenderSize(70f, 70f * 2)
    remote.teleport(startX, startY)
    // setPosition(x, y, Align.bottomLeft)
  }

  val stand = Animation(1.0f, assets.pack.newSprite("player/alienGreen_stand"))
  val jump = Animation(1.0f, assets.pack.newSprite("player/alienGreen_jump"))
  val walk = Animation(0.1f, assets.pack.newSprite("player/alienGreen_walk1"), assets.pack.newSprite("player/alienGreen_walk2"))

  init {
    walk.playMode = Animation.PlayMode.LOOP
  }
}

class GameControls(buttons: ButtonList) : BaseGameControls() {
  val left = buttons.newButton().addKeyboard(Input.Keys.LEFT).addKeyboard(Input.Keys.A)
  val right = buttons.newButton().addKeyboard(Input.Keys.RIGHT).addKeyboard(Input.Keys.D)
  val up = buttons.newButton().addKeyboard(Input.Keys.UP).addKeyboard(Input.Keys.W)
  val down = buttons.newButton().addKeyboard(Input.Keys.DOWN).addKeyboard(Input.Keys.S)
  val jump = buttons.newButton().addKeyboard(Input.Keys.X).addKeyboard(Input.Keys.SPACE)
}

class SpacemanWorld(args:WorldArg) : World(args) {
  val controls = GameControls(buttons)
}

class SpacemanSuperGame(game:Game) : SuperGame(game) {
  private val assets = Assets()
  private val worldCreators = CreatorList<SpacemanSuperGame, SpacemanWorld>(this)
      .registerCreator("alien-body", object: ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(game: SpacemanSuperGame, world: SpacemanWorld, map: ObjectCreatorDispatcher, x: Float, y: Float, tile: TiledMapTileMapObject) {
          val alien = Alien(assets, world, x, y)
          map.addObject(alien.stand, world, alien)
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
            val world = SpacemanWorld(args)
            val bak = StaticRenderLayer(args.renderLayerArgs)
            var background = ImageActor(assets.blue_grass)
            background.setSize(HEIGHT, HEIGHT)
            background.setPosition(0f, 0f, Align.bottomLeft)
            bak.stage.addActor(background)
            background = ImageActor(assets.blue_grass)
            background.setSize(HEIGHT, HEIGHT)
            background.setPosition(HEIGHT, 0f, Align.bottomLeft)
            bak.stage.addActor(background)
            world.addLayer(bak)
            return world
          }
        },
        object:Loader<SpacemanWorld> {
          override fun worldLoaded(map: SpacemanWorld) {
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
