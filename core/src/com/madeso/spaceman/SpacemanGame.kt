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

  val blue_grass = dest.ret(Texture(Gdx.files.internal("blue_grass.png")))
  val pack = dest.ret(TextureAtlas(Gdx.files.internal("pack.atlas")))
  val touch = dest.ret(TextureAtlas(Gdx.files.internal("touch/pack.atlas")))

  override fun dispose() {
    dest.dispose()
  }
}

val DMG_SPEED = 70f * 12f
val DMG_TIME = 0.1f
val FLICKER_TIME = 2.0f

val MOVE_SPEED = 70f * 6f
val JUMP_SPEED = 70f * 6
val GRAVITY = 70f * 32
val JUMP_KILL = 700f
val MAX_JUMP_TIME = 0.5f
val GHOST_JUMP = 0.1f
val MOVEMENT_ACCELERATION_GROUND = 0.6f
val MOVEMENT_ACCELERATION_AIR = 0.1f

val MAX_Y_SPEED = JUMP_SPEED * 4

class Alien(assets:Assets, d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) : ObjectControllerAnim  {
  private val world = d.world
  private val startX = d.startX
  private val startY = d.startY

  private var vy = 0f
  private var jumpTime = 0f
  private var curSpeed = 0f

  fun jumpKill() {
    vy = JUMP_KILL
  }

  fun  springBoardJump(alienRemote: ObjectRemote) {
    val test = -vy
    vy = Within(JUMP_KILL, test *
        if( world.controls.jump.isDown ) 1.4f
        else 0.8f
        , JUMP_KILL * 2.5f)
  }

  val isFalling : Boolean
    get() = vy < -40f

  var damageDir = 0
  var damageTime = 0f

  fun damage(otherX:Float?, remote:ObjectRemote) {
    if( otherX != null ) {
      damageDir =
          if (otherX < remote.x) {
            1
          } else {
            -1
          }

      damageTime = DMG_TIME
    }
  }

  override fun act(delta: Float, remote: ObjectRemote) {
    if( remote.lastCollision.down && world.controls.jump.isDown == false) {
      vy = 0f
      jumpTime = 0f
    }

    if( remote.lastCollision.x ) {
      curSpeed = 0f
    }

    if( damageTime > 0f ) damageTime -= delta
    val isTakingDamage = damageTime > 0f

    if( isTakingDamage == false && world.controls.jump.isClicked && jumpTime < GHOST_JUMP ) {
      vy = JUMP_SPEED
    }

    if( isTakingDamage == false && vy > 0f && world.controls.jump.isDown && jumpTime < MAX_JUMP_TIME) {
      // val exp = ( MAX_JUMP_TIME - jumpTime ) / MAX_JUMP_TIME
      // vy = JUMP_SPEED * exp*exp
      vy -= GRAVITY * delta * (jumpTime / MAX_JUMP_TIME) * (jumpTime / MAX_JUMP_TIME)
    }
    else {
      vy -= GRAVITY * delta
    }

    if( vy < -MAX_Y_SPEED) vy = -MAX_Y_SPEED

    jumpTime += delta

    val hor_move = if( isTakingDamage ) damageDir else PlusMinus(world.controls.right.isDown, world.controls.left.isDown)

    val targetSpeed = hor_move.toFloat() * if(isTakingDamage) DMG_SPEED else MOVE_SPEED

    val acc = if (isTakingDamage || remote.lastCollision.down) {
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

    if( isTakingDamage ) {
      remote.setAnimation(hurt)
    }
    else if( remote.lastCollision.down == false) {
      remote.setAnimation(jump)
    }
    else if (hor_move != 0 && !remote.lastCollision.x) {
      remote.setAnimation(walk)
    }
    else {
      remote.setAnimation(stand)
    }

    if( !remote.lastCollision.x && isTakingDamage==false ) {
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

    world.renderWorld.cameraLogic.updatePlatformCamera(delta, remote.x+remote.collisionRect.dx, remote.y+remote.collisionRect.dy, remote.collisionRect.width, remote.collisionRect.height)
  }

  override fun dispose() {
  }

  override fun init(remote: ObjectRemote) {
    remote.debug = true
    remote.keepWithinHorizontalWorld = true
    remote.teleport(startX, startY)
    remote.collisionRect.dx = remote.collisionRect.width / 2f
    remote.setRenderScale(0.6f)
  }

  override val basic: Animation
    get() = stand

  val stand = Animation(1.0f, assets.pack.newSprite("player/alienGreen_stand"))
  val hurt = Animation(1.0f, assets.pack.newSprite("player/alienGreen_hit"))
  val jump = Animation(1.0f, assets.pack.newSprite("player/alienGreen_jump"))
  val walk = Animation(0.1f, assets.pack.newSprite("player/alienGreen_walk1"), assets.pack.newSprite("player/alienGreen_walk2")).setLooping()
}

class Coin(assets: Assets, d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) : ObjectControllerAnim  {
  private val world = d.world
  private val startX = d.startX
  private val startY = d.startY

  override fun init(remote: ObjectRemote) {
    remote.teleport(startX, startY)
    remote.collisionRect.height =36f
    remote.collisionRect.width =36f
    remote.collisionRect.dx = (70f - remote.collisionRect.width) / 2f
    remote.collisionRect.dy = (70f - remote.collisionRect.height) / 2f
  }

  override fun act(delta: Float, remote: ObjectRemote) {
  }

  override fun dispose() {
  }

  override val basic = Animation(1.0f, assets.pack.newSprite("items/coinGold"))
}

val SLIME_SPEED = 50f
val WORM_SPEED = 30f
val WORM_ANIM_SPEED = 0.8f

class WalkingBehaviour(private var direction: Direction, private val speed : Float) : Behaviour() {
  override fun act(delta: Float, remote: ObjectRemote) {
    if (remote.lastCollision.x) {
      direction.moveRight = !direction.moveRight
    }

    remote.facingRight = !direction.moveRight

    remote.move(
          (if (direction.moveRight) 1.0f else -1.0f) * speed * delta
          , 0.0f
      )
  }
}

class Direction : Behaviour() {
  override fun act(delta: Float, remote: ObjectRemote) {
  }

  var moveRight = true
}

interface HasDirection : ObjectControllerAnim {
  val direction : Direction
}

interface Jumpable : ObjectController {
  fun smash(remote: ObjectRemote)
}

class Slime(assets: Assets, d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) : ObjectControllerAnim, Jumpable, HasDirection {
  private val world = d.world
  private val startX = d.startX
  private val startY = d.startY

  override val direction = Direction()
  val walking = WalkingBehaviour(direction, WORM_SPEED)

  override fun init(remote: ObjectRemote) {
    remote.teleport(startX, startY)
    remote.collisionRect.height =34f
    remote.collisionRect.width =49f
    remote.collisionRect.dx = 0f
    remote.collisionRect.dy = 0f
  }

  var squashTimer = 0f

  override fun smash(remote: ObjectRemote) {
    squashTimer = 0.5f
  }

  override fun act(delta: Float, remote: ObjectRemote) {
    if( squashTimer > 0f ) {
      squashTimer -= delta

      if( squashTimer > 0f ) {
        remote.setAnimation(squashed)
      }
      else {
        remote.setAnimation(basic)
      }
    }
    else {
      walking.act(delta, remote)
    }
  }

  override fun dispose() {
  }

  override val basic = Animation(0.5f, assets.pack.newSprite("enemies/slime"), assets.pack.newSprite("enemies/slime_walk")).setLooping()
  val squashed = Animation(1.0f, assets.pack.newSprite("enemies/slime_squashed"))
}

class Worm(assets: Assets, d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) : ObjectControllerAnim, Jumpable, HasDirection {
  private val startX = d.startX
  private val startY = d.startY

  override val direction = Direction()
  val walking = WalkingBehaviour(direction, SLIME_SPEED)

  override fun init(remote: ObjectRemote) {
    remote.teleport(startX, startY)
    remote.collisionRect.height =23f
    remote.collisionRect.width =63f
    remote.collisionRect.dx = 0f
    remote.collisionRect.dy = 0f
  }

  override fun smash(remote: ObjectRemote) {
    KillEnemy(remote, dead)
  }

  override fun act(delta: Float, remote: ObjectRemote) {
    walking.act(delta, remote)
  }

  override fun dispose() {
  }

  override val basic = Animation(WORM_ANIM_SPEED, assets.pack.newSprite("enemies/worm"), assets.pack.newSprite("enemies/worm_walk")).setLooping()
  val dead = Animation(1.0f, assets.pack.newSprite("enemies/worm_dead"))
}

class Snake(assets: Assets, d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) : ObjectControllerAnim, Jumpable, HasDirection {
  private val startX = d.startX
  private val startY = d.startY

  override val direction = Direction()
  val walking = WalkingBehaviour(direction, SLIME_SPEED)

  override fun init(remote: ObjectRemote) {
    remote.teleport(startX, startY)
    remote.collisionRect.height =23f
    remote.collisionRect.width =63f
    remote.collisionRect.dx = 0f
    remote.collisionRect.dy = 0f
  }

  override fun smash(remote: ObjectRemote) {
    KillEnemy(remote, dead)
  }

  override fun act(delta: Float, remote: ObjectRemote) {
    walking.act(delta, remote)
  }

  override fun dispose() {
  }

  override val basic = Animation(WORM_ANIM_SPEED, assets.pack.newSprite("enemies/snake"), assets.pack.newSprite("enemies/snake_walk")).setLooping()
  val dead = Animation(1.0f, assets.pack.newSprite("enemies/snake_dead"))
}

class SpringBoard(assets: Assets, d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) : ObjectControllerAnim  {
  private val world = d.world
  private val startX = d.startX
  private val startY = d.startY

  override fun init(remote: ObjectRemote) {
    remote.teleport(startX, startY)
    remote.collisionRect.height =49f
    remote.collisionRect.width =70f
    remote.collisionRect.dx = 0f
    remote.collisionRect.dy = 0f
  }

  var squashTimer = 0f

  fun smash() {
    squashTimer = 0.5f
  }

  override fun act(delta: Float, remote: ObjectRemote) {
    if( squashTimer > 0f ) {
      squashTimer -= delta

      if( squashTimer > 0f ) {
        remote.setAnimation(squashed)
      }
      else {
        remote.setAnimation(basic)
      }
    }
  }

  override fun dispose() {
  }

  override val basic = Animation(1.0f, assets.pack.newSprite("items/springboardUp"))
  val squashed = Animation(1.0f, assets.pack.newSprite("items/springboardDown"))
}

class Spikes(assets: Assets, d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) : ObjectControllerAnim  {
  private val world = d.world
  private val startX = d.startX
  private val startY = d.startY

  override fun init(remote: ObjectRemote) {
    remote.teleport(startX, startY)
    remote.collisionRect.height =35f
    remote.collisionRect.width =70f
    remote.collisionRect.dx = 0f
    remote.collisionRect.dy = 0f
  }

  override fun act(delta: Float, remote: ObjectRemote) {
  }

  override fun dispose() {
  }

  override val basic = Animation(1.0f, assets.pack.newSprite("items/spikes"))
}

val FLY_SPEED = 70f

class EnemyDead(private val startX: Float, private val startY: Float, private val facingRight: Boolean) : ObjectController {
  override fun dispose() {
  }

  override fun init(remote: ObjectRemote) {
    remote.teleport(startX, startY)
    remote.collisionRect.height =34f
    remote.collisionRect.width =49f
    remote.collisionRect.dx = 0f
    remote.collisionRect.dy = 0f
    remote.keepWithinHorizontalWorld = false
    remote.collideWithWorld = false
  }

  private var vy = 400f

  override fun act(delta: Float, remote: ObjectRemote) {
    remote.facingRight = facingRight
    remote.move(0f, Within(-64f, vy * delta, 64f))
    vy -= delta * GRAVITY
    if( remote.y < 0.0f ) {
      remote.removeSelf()
    }
  }
}

fun KillEnemy(remote: ObjectRemote, dead: Animation) {
  remote.removeSelf()
  remote.newObject(dead, EnemyDead(remote.x, remote.y, remote.facingRight))
}

class EnemyFly(assets: Assets, d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) : ObjectControllerAnim, Jumpable {
  private val world = d.world
  private val startX = d.startX
  private val startY = d.startY
  private val path = d.path

  override fun init(remote: ObjectRemote) {
    remote.debug = true
    remote.teleport(startX, startY)
    remote.collisionRect.height =34f
    remote.collisionRect.width =49f
    remote.collisionRect.dx = 0f
    remote.collisionRect.dy = 0f
  }

  private var current = 0.0f

  override fun act(delta: Float, remote: ObjectRemote) {
    if( path == null ) return;
    current = path.moveCurrent(current, delta * FLY_SPEED)
    //Gdx.app.log("move", "current is $current")
    val p = path.getPosition(current)
    remote.moveTo(p.x, p.y, true)
  }

  override fun smash(remote: ObjectRemote) {
    KillEnemy(remote, dead)
  }

  override fun dispose() {
  }

  override val basic = Animation(0.2f, assets.pack.newSprite("enemies/fly"), assets.pack.newSprite("enemies/fly_fly")).setLooping()
  val dead = Animation(0.2f, assets.pack.newSprite("enemies/fly_dead"))
}

private fun ObjectRemote.moveTo(tx: Float, ty: Float, updateDirection:Boolean) {
  val dx = tx - this.x
  val dy = ty - this.y
  this.move(dx, dy)
  if( updateDirection ) {
    if( dx > 0 ) this.facingRight = false
    else if ( dx < 0 ) this.facingRight = true
  }
  //Gdx.app.log("move", "moved to $tx $ty")
}

class SetDir(assets: Assets, d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>, val isRight : Boolean) : ObjectControllerAnim  {
  private val world = d.world
  private val startX = d.startX
  private val startY = d.startY

  override fun init(remote: ObjectRemote) {
    remote.teleport(startX, startY)
    remote.visible = false
    remote.collisionRect.height =70f
    remote.collisionRect.width =70f
    remote.collisionRect.dx = 0f
    remote.collisionRect.dy = 0f
  }

  override fun act(delta: Float, remote: ObjectRemote) {
  }

  override fun dispose() {
  }

  override val basic = Animation(0.5f, assets.pack.newSprite("enemies/slime"))
}

class GameControls(assets: Assets, ui: Ui, buttons: ButtonList) : BaseGameControls() {
  val left = buttons.newButton().addKeyboard(Input.Keys.LEFT).addKeyboard(Input.Keys.A).addGfx(ui, assets.touch, "flat", "action-left", Alignment.BOTTOM_LEFT, 0f, 0f)
  val right = buttons.newButton().addKeyboard(Input.Keys.RIGHT).addKeyboard(Input.Keys.D).addGfx(ui, assets.touch, "flat", "action-right", Alignment.BOTTOM_LEFT, 80f + ui.spacing, 0f)
  val up = buttons.newButton().addKeyboard(Input.Keys.UP).addKeyboard(Input.Keys.W)
  val down = buttons.newButton().addKeyboard(Input.Keys.DOWN).addKeyboard(Input.Keys.S)
  val jump = buttons.newButton().addKeyboard(Input.Keys.X).addKeyboard(Input.Keys.SPACE).addGfx(ui, assets.touch, "flat", "action-x", Alignment.BOTTOM_RIGHT, 0f, 0f)
}

class SpacemanWorld(assets: Assets, args:WorldArg) : World(args) {
  val controls = GameControls(assets, ui, buttons)
  var alien : Alien? = null

  init {
    renderWorld.camera.zoom = 0.6f

    addCollision(object: Collision<Alien, Coin>() {
      override fun onCollided(alien: Alien, alienRemote: ObjectRemote, coin: Coin, coinRemote: ObjectRemote) {
        coinRemote.removeSelf()
      }
    })

    addCollision(object: Collision<Alien, Jumpable>() {
      override fun onCollided(alien: Alien, alienRemote: ObjectRemote, slime: Jumpable, slimeRemote: ObjectRemote) {
        if( alienRemote.worldCollisionRect.y > slimeRemote.worldCollisionRect.y + slimeRemote.worldCollisionRect.height/2f ) {
          if( alien.isFalling ) {
            alien.jumpKill()
            slime.smash(slimeRemote)
          }
        }
        else {
          if( alienRemote.isFlickering == false ) {
            alienRemote.flicker(FLICKER_TIME)
            alien.damage(slimeRemote.x, alienRemote)
          }
        }
      }
    })

    addCollision(object: Collision<Alien, Spikes>() {
      override fun onCollided(alien: Alien, alienRemote: ObjectRemote, spikes: Spikes, spikesRemote: ObjectRemote) {
        if( alienRemote.worldCollisionRect.y > spikesRemote.worldCollisionRect.y + spikesRemote.worldCollisionRect.height/2f ) {
          alien.jumpKill()
          if( alienRemote.isFlickering == false ) {
            alienRemote.flicker(FLICKER_TIME)
            alien.damage(null, alienRemote)
          }
        }
      }
    })

    addCollision(object: Collision<SetDir, HasDirection>() {
      override fun onCollided(setDir: SetDir, setDirRemote: ObjectRemote, slime: HasDirection, slimeRemote: ObjectRemote) {
        slime.direction.moveRight = setDir.isRight
      }
    })

    addCollision(object: Collision<Alien, SpringBoard>() {
      override fun onCollided(alien: Alien, alienRemote: ObjectRemote, slime: SpringBoard, slimeRemote: ObjectRemote) {
        if( alienRemote.worldCollisionRect.y > slimeRemote.worldCollisionRect.y + slimeRemote.worldCollisionRect.height/2f ) {
          if( alien.isFalling ) {
            alien.springBoardJump(alienRemote)
            slime.smash()
          }
        }
      }
    })
  }

  override fun update(delta: Float) {
    super.update(delta)
  }
}

class SpacemanSuperGame(game:Game) : SuperGame(game) {
  private val assets = Assets()
  private val worldCreators = CreatorList<SpacemanSuperGame, SpacemanWorld>(this)
      .registerCreator("alien-body", object : ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          val alien = Alien(assets, d)
          d.world.alien = alien
          AddObject(d, alien)
        }
      })
      .registerNullCreator("alien-head")
      .registerCreator("worm", object : ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          AddObject(d, Worm(assets, d))
        }
      })
      .registerCreator("snake", object : ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          AddObject(d, Snake(assets, d))
        }
      })
      .registerCreator("gold-coin", object : ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          AddObject(d, Coin(assets, d))
        }
      })
      .registerCreator("spikes", object : ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          AddObject(d, Spikes(assets, d))
        }
      })
      .registerCreator("slime",object: ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          AddObject(d, Slime(assets, d))
        }
      })
      .registerCreator("fly",object: ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          AddObject(d, EnemyFly(assets, d))
        }
      })
      .registerCreator("dir-left",object: ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          AddObject(d, SetDir(assets, d, false))
        }
      })
      .registerCreator("dir-right",object: ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          AddObject(d, SetDir(assets, d, true))
        }
      })
      .registerCreator("springboard",object: ObjectCreator<SpacemanSuperGame, SpacemanWorld> {
        override fun create(d: ObjectCreateData<SpacemanSuperGame, SpacemanWorld>) {
          AddObject(d, SpringBoard(assets, d))
        }
      })

  init {
    loadWorld("test.tmx")
  }

  fun loadWorld(path:String) {
    val self = this
    setScreen(BasicLoaderScreen<SpacemanWorld>("test.tmx", worldCreators,
        object:WorldCreator<SpacemanWorld> {
          override fun createWorld(args: WorldArg): SpacemanWorld {
            val world = SpacemanWorld(assets, args)
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
