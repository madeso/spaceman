package com.madeso.spaceman

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Vector2
import java.util.*


class OrthoMap(private val assetManager: AssetManager, private val path: String) : SuperAsset {
  private var renderer: OrthogonalTiledMapRenderer? = null
  private var map: TiledMap? = null

  interface ObjectCreator {
    fun create(map: OrthoMap, x: Float, y: Float, tile: TiledMapTileMapObject)
  }

  init {
    this.assetManager.load(path, TiledMap::class.java)
  }

  override fun postLoad() {
    this.map = this.assetManager.get(path)
    val unitScale = 1f
    this.renderer = OrthogonalTiledMapRenderer(map, unitScale)

    val layer = this.map!!.layers.get("obj")
    if( layer == null ) throw Exception("Failed to get obj layer")
    val objs = layer.objects
    for (i in 0..objs.count - 1) {
      val obj = objs.get(i)
      if( obj is TiledMapTileMapObject) {
        var prop = obj.properties.get("type");
        if( prop == null ) prop = obj.tile.properties.get("type")
        if( prop != null ) {
          val type = prop.toString()
          getCreator(type).create(this, obj.x, obj.y, obj)
        }
      }
    }
  }

  internal var creators = HashMap<String, ObjectCreator>()

  private fun getCreator(tile: String): ObjectCreator {
    val c = creators.get(tile) ?: throw NullPointerException("Missing tile index " + tile)
    return c
  }

  internal fun registerCreator(tile: String, creator: ObjectCreator) {
    creators.put(tile, creator)
  }

  internal fun registerNullCreator(tile: String) {
    registerCreator(tile, object : ObjectCreator {
      override fun create(map: OrthoMap, x: Float, y: Float, tile: TiledMapTileMapObject) {
      }
    })
  }

  override fun dispose() {
    this.renderer!!.dispose()
    this.assetManager.unload(path)
  }

  fun render(camera: OrthographicCamera) {
    this.renderer!!.setView(camera)
    this.renderer!!.render()
  }

  fun sweptAABB(px: Float, py: Float, tx: Float, ty: Float, w: Float, h: Float): SweptCollisionUtil.ColResult {
    val col = this.map!!.layers.get("col") as TiledMapTileLayer
    for (x in 0..col.width - 1) {
      for (y in 0..col.height - 1) {
        val cell = col.getCell(x, y)
        if (cell != null) {
          if (cell.tile != null) {
            val box = SweptCollisionUtil.Box(px, py, tx, ty, w, h)
            val res = SweptCollisionUtil.Simple(box, SweptCollisionUtil.Box(x * col.tileWidth, y * col.tileHeight, col.tileWidth, col.tileHeight))
            if (res.ret < ONE) {
              return res
            }
          }
        }
      }
    }

    return SweptCollisionUtil.ColResult(tx, ty)
  }

  fun basic(x: Float, y: Float, tx: Float, ty: Float, w: Float, h: Float): CollisionData {
    val p = Vector2(x, y)
    val d = Vector2(tx - x, ty - y)
    val flags = BasicCollision.Simple(this.map!!, p, d, w, h)
    return CollisionData(p.x, p.y, flags)
  }

  fun slide(px: Float, py: Float, ptx: Float, pty: Float, w: Float, h: Float): CollisionData {
    var fx = px
    var fy = py
    val tx = ptx
    val ty = pty
    for (i in 0..0) {
      val res = sweptAABB(fx, fy, tx, ty, w, h)
      fx = res.x
      fy = res.y
      if (res.ret >= ONE)
        break
      else {
        val remainingtime = 1 - res.ret
        val dotprod = (res.vx * res.normaly + res.vy * res.normalx) * remainingtime
        val vx = dotprod * res.normaly
        val vy = dotprod * res.normalx
        //tx = fx + vx;
        //ty = fy + vy;
      }
    }

    return CollisionData(fx, fy)
  }

  companion object {

    private val ONE = 0.99f
  }
}
