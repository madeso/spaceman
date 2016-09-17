package com.madeso.spaceman

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import java.util.*

interface PostLoader {
  fun postLoad()
}

interface SuperAsset : Disposable, PostLoader

class SmartSound(private val assetManager: AssetManager, private val name: String) : SuperAsset {
  var sound: Sound? = null

  init {
    this.assetManager.load(name, Sound::class.java)
  }

  override fun postLoad() {
    this.sound = assetManager.get(name)
  }

  override fun dispose() {
    assetManager.unload(name)
  }

  fun play() {
    sound?.stop()
    sound?.play()
  }
}

fun <T> List<T>.asGdxArray(): Array<T> {
  var ret = Array<T>(size)
  for(i in this.indices) {
    ret[i] = this[i]
  }
  return ret
}

class SmartAnimation(private val assetManager: AssetManager, private var atlasName: String, private var keyFrames: List<String>) : SuperAsset {
  private var anim : Animation? = null

  init {
    this.assetManager.load(atlasName, TextureAtlas::class.java)
  }

  override fun postLoad() {
    var atlas : TextureAtlas = assetManager.get(atlasName)
    var frames = this.keyFrames.map { value -> atlas.findRegion(value) }.asGdxArray()
    this.anim = Animation(0.55f, frames)
  }

  override fun dispose() {
    this.assetManager.unload(atlasName)
  }

  var stateTime = 0f

  /*var regionWidth = anim.keyFrames[0].regionWidth.toFloat()
  var regionHeight = anim.keyFrames[0].regionHeight.toFloat()*/

  public fun act(delta:Float) {
    stateTime += delta;
  }

  public fun reset() {
    stateTime = 0f
  }

  fun image(): TextureRegion {
    return anim?.getKeyFrame(stateTime)!!
  }
}


class SmartAssetManager : Disposable {
  private val assetManager = AssetManager()
  private val assetsToLoad = ArrayList<PostLoader>()

  init {
    assetManager.setLoader(TiledMap::class.java, TmxMapLoader(InternalFileHandleResolver()))
  }

  fun postLoad() {
    val assets = ArrayList<PostLoader>(assetsToLoad)
    this.assetsToLoad.clear()
    for (asset in assets) {
      asset.postLoad()
    }
    assets.clear()
  }

  val isFinished: Boolean
    get() = this.assetsToLoad.isEmpty()

  fun onPostLoad(pl: PostLoader) {
    assetsToLoad.add(pl)
  }

  fun finishLoading() {
    this.assetManager.finishLoading()
  }

  fun update(): Boolean {
    return this.assetManager.update()
  }

  override fun dispose() {
    this.assetManager.dispose()
  }

  val progress: Float
    get() = this.assetManager.progress

  fun orthoMap(destructor: Destructor, path: String): OrthoMap {
    val map = OrthoMap(this.assetManager, path)
    assetsToLoad.add(map)
    destructor.add(map)
    return map
  }

  fun animation(destructor: Destructor, name: String, vararg keyFrames : String): SmartAnimation {
    val animation = SmartAnimation(this.assetManager, name, keyFrames.toList())
    assetsToLoad.add(animation)
    destructor.add(animation)
    return animation
  }

  fun sound(destructor: Destructor, name: String): SmartSound {
    val sound = SmartSound(this.assetManager, name)
    assetsToLoad.add(sound)
    destructor.add(sound)
    return sound
  }
}
