package com.madeso.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.StretchViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.madeso.engine.collision.CollisionFlags
import com.madeso.engine.collision.CollisionMap
import com.madeso.engine.collision.Collison_basic
import java.util.*


interface ObjectRemote {
  fun setAnimation(animation:Animation)
  fun removeSelf()
  fun flicker(timer:Float)
  fun teleport(x:Float, y:Float)
  fun move(dx:Float, dy:Float)

  fun setRenderSize(width : Float, height : Float)
  var debug : Boolean
  val outside : CollisionFlags
  var collisionRect : CollisionRect

  val x : Float
  val y : Float
  val width : Float
  val height : Float

  var keepWithinHorizontalWorld : Boolean

  val lastCollision : CollisionFlags

  var facingRight : Boolean
}

interface ObjectController : Disposable {
  fun init(remote: ObjectRemote)
  fun act(delta:Float, remote:ObjectRemote)
}

interface ObjectCreatorDispatcher {
  fun addObject(ani: Animation, world: World, obj: ObjectController)
}

interface ObjectCreator<TGame : SuperGame, TWorld:World> {
  fun create(game: TGame, world: TWorld, map: ObjectCreatorDispatcher, x: Float, y: Float, tile: TiledMapTileMapObject)
}

interface CreatorMap<TWorld:World> {
  fun getCreator(tileName: String, world: TWorld, map: ObjectCreatorDispatcher, x: Float, y: Float, tile: TiledMapTileMapObject)
}

class CreatorList<TGame : SuperGame, TWorld:World>(private var game: TGame) : CreatorMap<TWorld> {
  private var creators = HashMap<String, ObjectCreator<TGame, TWorld>>()

  override fun getCreator(tileName: String, world: TWorld, map: ObjectCreatorDispatcher, x: Float, y: Float, tile: TiledMapTileMapObject) {
    val creator = creators.get(tileName) ?: throw NullPointerException("Missing creator for " + tileName)
    creator.create(game, world, map, x, y, tile)
  }

  fun registerCreator(tile: String, creator: ObjectCreator<TGame, TWorld>) : CreatorList<TGame, TWorld> {
    creators.put(tile, creator)
    return this
  }

  fun registerNullCreator(tile: String) : CreatorList<TGame, TWorld> {
    return registerCreator(tile, object : ObjectCreator<TGame, TWorld> {
      override fun create(game: TGame, world: TWorld, map: ObjectCreatorDispatcher, x: Float, y: Float, tile: TiledMapTileMapObject) {
      }
    })
  }
}

class NoCollisions : CollisionMap {
  override fun cellAt(x: Int, y: Int): Boolean {
    return false
  }

  override val width: Int = 10
  override val height: Int = 10
  override val tileWidth: Float = 10f
  override val tileHeight: Float = 10f

}

interface WorldCreator<TWorld:World> {
  fun createWorld(args:WorldArg) : TWorld
}

/**
 * The is responsible for loading the map and passing its data along to the world renderer and updator
 */
class LoadingMap<TWorld:World>(private val assetManager: AssetManager, private val creators : CreatorMap<TWorld>, private val path: String) : Disposable {
  init {
    this.assetManager.load(path, TiledMap::class.java)
  }

  fun postLoad(worldCreator:WorldCreator<TWorld>) : TWorld {
    val map : TiledMap = this.assetManager.get(path)
    val unitScale = 1f

    val rw = RenderWorld()
    val collisionLayer = map.getLayers().get("col")
    val objects = UpdateWorld(
        if(collisionLayer != null )
          TiledCollisionMap(collisionLayer)
        else
          NoCollisions()
    )

    val renderLayerArgs = RenderLayerArgs(rw.batch, rw.viewport, rw.camera)
    val world = worldCreator.createWorld(WorldArg(rw, objects, "", renderLayerArgs))

    var addedTiles = false

    for(layer in map.layers) {
      // cast layers
      if( layer is TiledMapTileLayer ) {
        // is tile layer...
        world.width = Math.max(world.width, layer.width * layer.tileWidth)
        world.height = Math.max(world.height, layer.height * layer.tileHeight)

        rw.layers.add(TileRenderLayer( map, layer, renderLayerArgs))
      }
      else {
        // is object layer
        val objectRenderLayer = ObjectRenderLayer(renderLayerArgs)
        rw.layers.add(objectRenderLayer)

        val dispatcher = object : ObjectCreatorDispatcher {
          override fun addObject(ani: Animation, world: World, controller: ObjectController) {
            val rend = PhysicalWorldObjectRenderer()
            val obj = PhysicalWorldObject(ani, world, controller, rend)
            rend.master = obj

            objectRenderLayer.stage.addActor(rend)
            objects.add(obj)
          }
        }

        val objs = layer.objects
        for (i in 0..objs.count - 1) {
          val obj = objs.get(i)
          if( obj is TiledMapTileMapObject) {
            var prop = obj.properties.get("type");
            if( prop == null ) prop = obj.tile.properties.get("type")
            if( prop != null ) {
              val type = prop.toString()
              creators.getCreator(type, world, dispatcher, obj.x, obj.y, obj)
            }
          }
        }
      }

    }

    return world
  }

  override fun dispose() {
    this.assetManager.unload(path)
  }
}

class WorldArg(val renderWorld : RenderWorld, val updateWorld : UpdateWorld, val backgroundName : String, val renderLayerArgs:RenderLayerArgs) {
}

abstract class World(args:WorldArg) {
  val renderWorld = args.renderWorld
  val updateWorld = args.updateWorld
  var width = 0f
  var height = 0f
  val ui = Ui()
  protected val buttons = ButtonList()

  open fun update(delta:Float) {
    ui.act(delta)
    buttons.act(delta)
    updateWorld.act(delta)
  }

  fun addLayer(layer:RenderLayer) {
    renderWorld.layers.add(layer)
  }

  fun render(delta: Float) {
    renderWorld.render(delta)
    ui.render(delta)
  }

  fun resize(width: Int, height: Int) {
    ui.resize(width, height)
    renderWorld.resize(width, height)
  }
}

interface Loader<TWorld:World> {
  fun worldLoaded(map:TWorld)
  // functions for rendering?
}

class WorldScreen(private val world : World) : ScreenAdapter() {
  override fun render(delta: Float) {
    super.render(delta)
    world.update(delta)
    world.render(delta)
  }

  override fun resize(width: Int, height: Int) {
    super.resize(width, height)
    world.resize(width, height)
  }

}

class BasicLoaderScreen<TWorld:World>(path: String, creators: CreatorMap<TWorld>, private val worldCreator : WorldCreator<TWorld>, private val loader : Loader<TWorld>) : ScreenAdapter() {
  private var loaded = false
  private val assetManager = AssetManager()
  private val map : LoadingMap<TWorld>

  init {
    assetManager.setLoader(TiledMap::class.java, TmxMapLoader(InternalFileHandleResolver()))
    map = LoadingMap<TWorld>(assetManager, creators, path)
  }

  override fun render(delta: Float) {
    ClearScreen()

    if (this.loaded) {
      if (true) { // Gdx.input.isTouched()) {
        assetManager.finishLoading()

        loader.worldLoaded( map.postLoad(worldCreator) )
      }
    } else {
      if (assetManager.update()) {
        assetManager.finishLoading()
        this.loaded = true
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

interface RenderLayer : Disposable  {
  fun render(delta:Float)
  fun resize(width: Int, height: Int)
}

class RenderLayerArgs(val batch: Batch, val viewport: Viewport, val camera: OrthographicCamera) {
}

class StaticRenderLayer(args:RenderLayerArgs) : RenderLayer {
  private val batch = args.batch
  private val camera = OrthographicCamera()
  private val viewport = StretchViewport(WIDTH, HEIGHT, camera)

  val stage = Stage(viewport, batch)

  override fun dispose() {
    stage.dispose()
  }

  override fun render(delta: Float) {
    viewport.apply()
    batch.projectionMatrix = camera.combined;
    stage.act(delta)
    stage.draw()
  }

  override fun resize(width: Int, height: Int) {
    viewport.update(width, height)
  }
}

class ObjectRenderLayer(args: RenderLayerArgs) : RenderLayer{
  private val batch = args.batch
  private val camera = args.camera
  private val viewport = args.viewport

  val stage = Stage(viewport, batch)

  override fun dispose() {
    stage.dispose()
  }

  override fun render(delta: Float) {
    stage.act(delta)

    viewport.apply()
    batch.projectionMatrix = camera.combined;
    stage.draw()
  }

  override fun resize(width: Int, height: Int) {
  }
}

class TileRenderLayer(val map: TiledMap, private val layer: TiledMapTileLayer, args: RenderLayerArgs) : RenderLayer{
  private val batch = args.batch
  private val viewport = args.viewport
  private val camera = args.camera

  private val renderer = OrthogonalTiledMapRenderer(map, batch)
  override fun render(delta: Float) {
    viewport.apply()
    batch.projectionMatrix = camera.combined;
    renderer.setView(camera)
    batch.begin()
    renderer.renderTileLayer(layer)
    batch.end()
  }

  override fun dispose() {
    renderer.dispose()
  }

  override fun resize(width: Int, height: Int) {
  }
}

private fun Smooth(dt: Float, now: Float, smoothTime: Float, next: Float): Float {
  val distance = next - now
  val speed = distance / smoothTime
  return now + speed * dt
}

enum class CameraMode {
  FREEFORM, ZERO, PLATFORM
}

private fun NiceValue(x: Float): Float {
  // hides the issue with ugly lines appearing between the tiles
  // shamelessly stolen from: http://www.reddit.com/r/gamedev/comments/1euvrs/libgdx_tiledmap_linesgaps_between_tiles/
  val smooth = 5.0f
  return MathUtils.round(smooth * x) / smooth
}

private fun isWithin(x:Float, cx:Float, w:Float, cw: Float) : Boolean {
  if( x <= cx ) return false
  else if( x >= cx+cw-w) return false
  else return true
}

class CameraCode(private var camera: OrthographicCamera) {
  private var cameraX = 0f
  private var cameraY = 0f
  private var cameraMode = CameraMode.FREEFORM

  private var width = 0f
  private var height = 0f

  private var trapWidth = 0f
  private var trapHeigtht = 0f

  private var trapX = 0f

  private val SMOOTH_TIME = 0.25f

  fun updatePlatformCamera(delta:Float, x:Float, y:Float, width:Float, height:Float) {

    // debugging
    this.width = width
    this.height = height
    trapWidth = width * 2f
    trapHeigtht = height * 2f

    val (targetX, targetY) = getFreeformCameraTarget(height, width, x, y)

    cameraX = Smooth(delta, cameraX, SMOOTH_TIME, targetX)
    cameraY = Smooth(delta, cameraY, SMOOTH_TIME, targetY)

    camera.position.x = NiceValue(cameraX)
    camera.position.y = NiceValue(cameraY)
  }

  private fun getFreeformCameraTarget(height: Float, width: Float, x: Float, y: Float): Pair<Float, Float> {
    val lookAhead = width*3

    val targetX =
        if ( isWithin(x, trapX, width, trapWidth) )
          cameraX
        else {
          if (x > trapX) {
            trapX = x - width
            trapX + lookAhead
          }
          else {
            trapX = x
            trapX - lookAhead
          }
        }
    val targetY =
        if (isWithin(y, cameraY, height, trapHeigtht))
          cameraY
        else {
          if (y > cameraY) y - height
          else y
        }

    // store trap position
    // make the trap stay behind

    return Pair(targetX, targetY)
  }

  fun debugRender(sprites: ShapeRenderer) {
    sprites.begin(ShapeRenderer.ShapeType.Line)
    sprites.color = Color.BLACK
    sprites.rect(trapX-cameraX, 0f, trapWidth, trapHeigtht)
    sprites.end()
  }
}

class RenderWorld : Disposable {
  val layers = Array<RenderLayer>()

  val camera = OrthographicCamera()
  val viewport = StretchViewport(WIDTH, HEIGHT, camera)
  val batch = SpriteBatch()

  override fun dispose() {
    for( layer in layers) {
      layer.dispose()
    }
  }

  private val sprites = ShapeRenderer()

  val cameraLogic = CameraCode(camera)

  fun render(delta:Float) {
    ClearScreen()

    camera.update();

    AnimatedTiledMapTile.updateAnimationBaseTime()

    for( layer in layers) {
      layer.render(delta)
    }

    viewport.apply()
    sprites.projectionMatrix = camera.projection

    cameraLogic.debugRender(sprites)
  }

  fun resize(width: Int, height:Int) {
    viewport.update(width, height)

    for( layer in layers) {
      layer.resize(width, height)
    }
  }
}

class TiledCollisionMap(maplayer: MapLayer) : CollisionMap {
  private val layer = maplayer as TiledMapTileLayer

  override val width: Int = layer.width
  override val height: Int = layer.height
  override val tileWidth: Float = layer.tileWidth
  override val tileHeight: Float = layer.tileHeight

  override fun cellAt(x: Int, y: Int): Boolean {
    val cell = layer.getCell(x, y)
    return cell != null
  }
}

abstract class WorldObject : Disposable {
  var shouldBeRemoved = false
    private set

  protected fun removeMe() {
    shouldBeRemoved = true
  }

  abstract fun act(delta:Float)
}

abstract class MoveableObject : WorldObject() {
  var rect = Rectangle()
      protected set
  abstract fun applyMovement(map: CollisionMap)
}

/**
 * The world is responsible for updating objects and calling all the onCollision functions.
 */
class UpdateWorld(private val collision: CollisionMap) : Disposable {
  private val destructor = Destructor()

  private val objects = Array<WorldObject>()
  private val moveables = Array<MoveableObject>()

  fun act(delta: Float) {
    for(o in objects) {
      o.act(delta)
    }

    for(m in moveables) {
      m.applyMovement(collision)
    }

    objects.removeAll { obj -> obj.shouldBeRemoved }
    moveables.removeAll { mov -> mov.shouldBeRemoved }
  }

  fun add(o : MoveableObject) {
    objects.add(o)
    moveables.add(o)
  }

  override fun dispose() {
    destructor.dispose()
  }
}

class PhysicalWorldObjectRenderer : Actor() {
  var master : PhysicalWorldObject? = null

  init {
    setOrigin(Align.bottom)
  }

  override fun act(delta: Float) {
    super.act(delta)
    val master = this.master
    if (master != null) {
      x = master.x
      y = master.y
    }
  }

  override fun draw(batch: Batch?, parentAlpha: Float) {
    super.draw(batch, parentAlpha)
    val master = this.master
    if( master == null ) throw Exception("master was null")
    master.render(batch ?: throw Exception("batch was null"), width, height)
  }

  override fun drawDebug(shapes: ShapeRenderer?) {
    super.drawDebug(shapes)
    val master = this.master
    if( master == null ) throw Exception("master was null")
    master.renderDebug(shapes?: throw Exception("shape render was null"))
  }
}

class CollisionRect {
  var dx = 0f
  var dy = 0f
  var width = 64f
  var height = 64f
}

class PhysicalWorldObject(private var animation : Animation, private val world: World, private var controller: ObjectController, val renderObject: PhysicalWorldObjectRenderer) : MoveableObject() {
  var x = 0f
    private set
  var y = 0f
    private set
  private var targetX = 0f
  private var targetY = 0f
  private var suggestedX = 0f
  private var suggestedY = 0f
  private val drawSuggested = false
  private val collideWithWorld = true
  var animationTime = 0f

  private var keepWithinHorizontalWorld = false

  protected var collisionRect = CollisionRect()

  val remote : ObjectRemote

  init {
    val self = this
    remote = object : ObjectRemote {
      override var collisionRect: CollisionRect
        get() = self.collisionRect
        set(value) {
          self.collisionRect = value
        }
      override val x: Float
        get() = self.x

      override val y: Float
        get() = self.y

      override val width: Float
        get() = self.renderObject.width

      override val height: Float
        get() = self.renderObject.height

      override val outside: CollisionFlags
        get() {
          val flags = CollisionFlags()

          if( x+renderObject.width < 0 ) flags.left = true
          if( x >  world.width ) flags.right = true
          if( y+renderObject.height < 0 ) flags.down = true
          if( y >  world.height) flags.up = true
          return flags
        }
      override var debug : Boolean
        get() {
          return renderObject.debug
        }
        set(value) {
          renderObject.debug = value
        }

      override var keepWithinHorizontalWorld: Boolean
        get() = self.keepWithinHorizontalWorld
        set(value) {
          self.keepWithinHorizontalWorld = value
        }

      override val lastCollision: CollisionFlags
        get() = latestFlags

      override fun setRenderSize(width: Float, height: Float) {
        self.renderObject.width = width
        self.renderObject.height = height
      }

      override fun removeSelf() {
        self.removeMe()
        self.renderObject.remove()
      }

      override fun flicker(timer: Float) {
        self.flicker(timer)
      }

      override fun teleport(x: Float, y: Float) {
        self.teleport(x, y)
      }

      override fun move(dx: Float, dy: Float) {
        self.move(dx, dy)
      }

      override var facingRight: Boolean
        get() {
          return self.isFacingRight
        }
        set(value) {
          self.isFacingRight = value
        }

      override fun setAnimation(animation: Animation) {
        self.setAnimation(animation)
      }
    }

    controller.init(remote)
  }

  protected fun setAnimation(animation: Animation) {
    if( this.animation == animation) return;
    this.animation = animation
    animationTime = 0f
  }

  protected var latestFlags = CollisionFlags()
  private var isFacingRight = true
  private var flickertimer = 0f
  private var ftimer = 0f

  fun teleport(x: Float, y: Float) {
    this.x = x
    this.y = y
    this.targetX = x
    this.targetY = y
  }

  fun move(dx: Float, dy: Float) {
    targetX += dx
    targetY += dy
  }

  override fun act(dt: Float) {
    if (this.flickertimer > 0) {
      this.flickertimer -= dt
    } else {
      this.flickertimer = 0f
    }
    this.ftimer += dt
    if (this.ftimer > FLICKER * 2) {
      this.ftimer -= FLICKER * 2
    }

    controller.act(dt, remote)

    animationTime += dt
  }

  override fun dispose() {
    controller.dispose()
  }

  fun render(batch: Batch, width: Float, height: Float) {
    if (isFlickering) {
      if (this.ftimer > FLICKER) {
        return
      }
    }

    val animationFrame = animation.getKeyFrame(animationTime)

    if (this.isFacingRight) {
      batch.draw(animationFrame, x, y, width, height)
    } else {
      batch.draw(animationFrame, x + width, y, -width, height)
    }


    if (this.drawSuggested) {
      batch.setColor(1f, 0f, 0f, 0.5f)
      batch.draw(animationFrame, suggestedX, suggestedY, width, width)
      batch.setColor(1f, 1f, 1f, 1f)
    }
  }

  fun renderDebug(d:ShapeRenderer) {
    d.rect(x + collisionRect.dx, y + collisionRect.dy, collisionRect.width, collisionRect.height)
  }

  val isFlickering: Boolean
    get() = this.flickertimer > 0

  override fun applyMovement(map: CollisionMap) {
    val cd = Collison_basic(map, x+collisionRect.dx, y+collisionRect.dy, targetX+collisionRect.dx, targetY+collisionRect.dy, collisionRect.width, collisionRect.height)
    suggestedX = cd.x - collisionRect.dx
    suggestedY = cd.y - collisionRect.dy
    this.latestFlags = cd.flags

    // update position
    if (this.collideWithWorld) {
      this.x = suggestedX
      this.y = suggestedY
    } else {
      this.x = targetX
      this.y = targetY
    }

    if( keepWithinHorizontalWorld) {
      if( this.x < 0f ) {
        this.x = 0f
        this.latestFlags.left = true
      }
      if( this.x  + renderObject.width > world.width) {
        this.x = world.width - renderObject.width
        this.latestFlags.right = true
      }
    }

    // update movement code
    this.targetX = this.x
    this.targetY = this.y
  }

  fun flicker(time: Float) {
    this.flickertimer = time
  }

  private val FLICKER = 0.02f
}

