package com.madeso.spaceman

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Pool
import com.badlogic.gdx.utils.Array

// stole from http://www.gamedev.net/page/resources/_/technical/game-programming/swept-aabb-collision-detection-and-response-r3084
object SweptCollisionUtil {
  class Box {
    constructor(x: Float, y: Float, tx: Float, ty: Float, w: Float, h: Float) {
      this.x = x
      this.y = y
      this.vx = tx - x
      this.vy = ty - y
      this.w = w
      this.h = h
    }

    constructor(x: Float, y: Float, w: Float, h: Float) {
      this.x = x
      this.y = y
      this.w = w
      this.h = h
      this.vx = 0f
      this.vy = 0f
    }

    // position of top-left corner
    var x: Float = 0.toFloat()
    var y: Float = 0.toFloat()

    // dimensions
    var w: Float = 0.toFloat()
    var h: Float = 0.toFloat()

    // velocity
    var vx: Float = 0.toFloat()
    var vy: Float = 0.toFloat()
  }

  fun Simple(box: Box, block: Box): ColResult {
    val r = SweptAABB(box, block)
    val collisiontime = r.ret
    r.x += box.vx * collisiontime
    r.y += box.vy * collisiontime

    r.vx = box.vx
    r.vy = box.vy
    return r
  }

  class ColResult {
    var normalx = 0f
    var normaly = 0f
    var ret = 0f

    var x = 0f
    var y = 0f

    var vx = 0.0f
    var vy = 0.0f

    constructor(normalx: Float, normaly: Float, ret: Float) {
      this.normalx = normalx
      this.normaly = normaly
      this.ret = ret
    }

    constructor(x: Float, y: Float) {
      this.normalx = 0f
      this.normaly = 0f
      this.x = x
      this.y = y
      this.ret = 1f
    }
  }

  fun SweptAABB(b1: Box, b2: Box): ColResult {
    val INF = 1000000000000.0f

    val xInvEntry: Float
    val yInvEntry: Float
    val xInvExit: Float
    val yInvExit: Float

    // find the distance between the objects on the near and far sides for both x and y
    if (b1.vx > 0.0f) {
      xInvEntry = b2.x - (b1.x + b1.w)
      xInvExit = b2.x + b2.w - b1.x
    } else {
      xInvEntry = b2.x + b2.w - b1.x
      xInvExit = b2.x - (b1.x + b1.w)
    }

    if (b1.vy > 0.0f) {
      yInvEntry = b2.y - (b1.y + b1.h)
      yInvExit = b2.y + b2.h - b1.y
    } else {
      yInvEntry = b2.y + b2.h - b1.y
      yInvExit = b2.y - (b1.y + b1.h)
    }

    // find time of collision and time of leaving for each axis (if statement is to prevent divide by zero)
    val xEntry: Float
    val yEntry: Float
    val xExit: Float
    val yExit: Float

    if (b1.vx == 0.0f) {
      xEntry = -INF
      xExit = INF
    } else {
      xEntry = xInvEntry / b1.vx
      xExit = xInvExit / b1.vx
    }

    if (b1.vy == 0.0f) {
      yEntry = -INF
      yExit = INF
    } else {
      yEntry = yInvEntry / b1.vy
      yExit = yInvExit / b1.vy
    }

    // find the earliest/latest times of collision
    val entryTime = Math.max(xEntry, yEntry)
    val exitTime = Math.min(xExit, yExit)

    // if there was no collision
    if (entryTime > exitTime || xEntry < 0.0f && yEntry < 0.0f || xEntry > 1.0f || yEntry > 1.0f) {
      return ColResult(0.0f, 0.0f, 1.0f)
    } else
    // if there was a collision
    {
      val normalx: Float
      val normaly: Float
      // calculate normal of collided surface
      if (xEntry > yEntry) {
        if (xInvEntry < 0.0f) {
          normalx = 1.0f
          normaly = 0.0f
        } else {
          normalx = -1.0f
          normaly = 0.0f
        }
      } else {
        if (yInvEntry < 0.0f) {
          normalx = 0.0f
          normaly = 1.0f
        } else {
          normalx = 0.0f
          normaly = -1.0f
        }
      }

      // return the time of collision
      return ColResult(normalx, normaly, entryTime)
    }
  }
}


object BasicCollision {
  private fun getTiles(map: TiledMap, startX: Int, startY: Int, endX: Int, endY: Int, tiles: Array<Rectangle>) {
    val layer = map.getLayers().get("col") as TiledMapTileLayer
    rectPool.freeAll(tiles)
    tiles.clear()
    for (y in startY..endY) {
      for (x in startX..endX) {
        val cell = layer.getCell(x, y)
        if (cell != null) {
          val rect = rectPool.obtain()
          rect.set(x.toFloat(), y.toFloat(), 1f, 1f)
          tiles.add(rect)
        }
      }
    }
  }


  private val rectPool = object : Pool<Rectangle>() {
    protected override fun newObject(): Rectangle {
      return Rectangle()
    }
  }

  private val tiles = Array<Rectangle>()

  private fun SubSimple(map: TiledMap, position: Vector2, velocity: Vector2, width: Float, height: Float): CollisionFlags {
    val flags = CollisionFlags()
    // perform collision detection & response, on each axis, separately
    // if the koala is moving right, check the tiles to the right of it's
    // right bounding box edge, otherwise check the ones to the left
    val koalaRect = rectPool.obtain()
    koalaRect.set(position.x, position.y, width, height)
    var startX: Int
    var startY: Int
    var endX: Int
    var endY: Int

    var right = true

    if (velocity.x > 0) {
      endX = (position.x + width + velocity.x) as Int
      startX = endX
      right = true
    } else {
      endX = (position.x + velocity.x) as Int
      startX = endX
      right = false
    }
    startY = position.y as Int
    endY = (position.y + height) as Int
    getTiles(map, startX, startY, endX, endY, tiles)
    koalaRect.x += velocity.x
    for (tile in tiles) {
      if (koalaRect.overlaps(tile)) {
        velocity.x = 0f
        if (right) {
          flags.right = true
        } else {
          flags.left = true
        }
        break
      }
    }
    koalaRect.x = position.x

    // if the koala is moving upwards, check the tiles to the top of it's
    // top bounding box edge, otherwise check the ones to the bottom
    if (velocity.y > 0) {
      endY = (position.y + height + velocity.y) as Int
      startY = endY
    } else {
      endY = (position.y + velocity.y) as Int
      startY = endY
    }
    startX = position.x as Int
    endX = (position.x + width) as Int
    getTiles(map, startX, startY, endX, endY, tiles)
    koalaRect.y += velocity.y
    for (tile in tiles) {
      if (koalaRect.overlaps(tile)) {
        // we actually reset the koala y-position here
        // so it is just below/above the tile we collided with
        // this removes bouncing :)
        if (velocity.y > 0) {
          position.y = tile.y - height
          // we hit a block jumping upwards, let's destroy it!
          // TiledMapTileLayer layer = (TiledMapTileLayer)map.getLayers().get(1);
          // layer.setCell((int)tile.x, (int)tile.y, null);
          flags.up = true
        } else {
          position.y = tile.y + tile.height
          // if we hit the ground, mark us as grounded so we can jump
          flags.down = true
        }
        velocity.y = 0f
        break
      }
    }
    rectPool.free(koalaRect)

    return flags
  }

  fun Simple(map: TiledMap, position: Vector2, velocity: Vector2, width: Float, height: Float): CollisionFlags {
    val tw = 64f
    val th = 64f
    position.x /= tw
    position.y /= th
    velocity.x /= tw
    velocity.y /= th

    val flags = SubSimple(map, position, velocity, width / tw, height / th)

    position.x *= tw
    position.y *= th
    velocity.x *= tw
    velocity.y *= th

    position.x += velocity.x
    position.y += velocity.y

    return flags
  }
}

class CollisionData {
  var flags = CollisionFlags()

  constructor(x: Float, y: Float) {
    this.x = x
    this.y = y
  }

  constructor(x: Float, y: Float, flags: CollisionFlags) {
    this.x = x
    this.y = y
    this.flags = flags
  }

  var x: Float = 0.toFloat()
  var y: Float = 0.toFloat()
}

class CollisionFlags {
  var down = false
  var up = false
  var right = false
  var left = false

  fun collided(): Boolean {
    return x() || y()
  }

  fun y(): Boolean {
    return up || down
  }

  fun x(): Boolean {
    return left || right
  }
}
