package com.madeso.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.StretchViewport

private val MAX_TOUCHES = 3

interface VirtualButton {
  val isDown : Boolean
}

class KeyboardButton(private val button: Int) : VirtualButton {
  override val isDown: Boolean
    get() = Gdx.input.isKeyPressed(button)
}

enum class Alignment {
  TOP_LEFT,
  TOP_CENTER,
  TOP_RIGHT,

  CENTER_LEFT,
  CENTER_CENTER,
  CENTER_RIGHT,

  BOTTOM_LEFT,
  BOTTOM_CENTER,
  BOTTOM_RIGHT,
}

class Ui {
  val camera = OrthographicCamera()
  val viewport = StretchViewport(WIDTH, HEIGHT, camera)
  val batch = SpriteBatch()
  val stage = Stage(viewport, batch)

  var scale = 1.5f

  fun act(delta: Float) {
    stage.act(delta)
  }

  fun render(delta: Float) {
    stage.draw()
  }

  fun resize(width: Int, height: Int) {
    viewport.update(width, height)
  }
}

class GfxButton(private val ui: Ui, atlas: TextureAtlas, style:String, type:String) : VirtualButton, Actor() {
  private var up = atlas.newSprite(style + "Dark/" + type )
  private var down = atlas.newSprite(style + "Light/" + type )

  init {
    ui.stage.addActor(this)
    width = up.originalWidth.toFloat()
    height = up.originalHeight.toFloat()
  }

  private var isTouched = false

  override fun act(delta: Float) {
    isTouched = false
    for(i in 0..MAX_TOUCHES) {
      val screen = Vector3(Gdx.input.getX(i).toFloat(), Gdx.input.getY(i).toFloat(), 0f)
      val world = ui.camera.unproject(screen)
      val contains = Rectangle(x, y, width, height).contains(world.x, world.y)
      if( contains ) {
        isTouched = true
      }
    }
  }

  override fun draw(batch: Batch?, parentAlpha: Float) {
    super.draw(batch, parentAlpha)
    val img =
        if(isTouched) { down }
        else { up }
    batch!!.draw(img, x, y, width, height)
  }

  override val isDown: Boolean
    get() = isTouched
}

fun align(left: Boolean, middle: Boolean, right: Boolean, x:Float, w:Float) : Float {
  if( left ) {
    assert(middle == false && right == false)
    return x
  }
  else if ( middle ) {
    assert(right == false)
    return x - w / 2f
  }
  else {
    assert(right == true)
    return x - w
  }
}

fun AnyOf(a: Alignment, v1: Alignment, v2: Alignment, v3: Alignment) = a == v1  || a == v2 || a==v3

class Button {
  var isDown = false
    private set

  private var wasDown = false

  var isClicked = false
    private set

  private val keyboardButtons = Array<VirtualButton>()

  fun addKeyboard(button:Int) : Button {
    keyboardButtons.add(KeyboardButton(button))
    return this
  }

  fun addGfx(ui: Ui, atlas: TextureAtlas, style: String, type: String, alignment: Alignment, x: Float, y: Float): Button {
    val button = GfxButton(ui, atlas, style, type)
    button.width *= ui.scale
    button.height *= ui.scale
    button.x =
        align(
          AnyOf(alignment, Alignment.BOTTOM_LEFT, Alignment.CENTER_LEFT, Alignment.TOP_LEFT),
          AnyOf(alignment, Alignment.BOTTOM_CENTER, Alignment.CENTER_CENTER, Alignment.TOP_CENTER),
          AnyOf(alignment, Alignment.BOTTOM_RIGHT, Alignment.CENTER_RIGHT, Alignment.TOP_RIGHT),
        x, button.width)
    button.y = align(
        AnyOf(alignment, Alignment.BOTTOM_LEFT, Alignment.BOTTOM_CENTER, Alignment.BOTTOM_RIGHT),
        AnyOf(alignment, Alignment.CENTER_LEFT, Alignment.CENTER_CENTER, Alignment.CENTER_RIGHT),
        AnyOf(alignment, Alignment.TOP_LEFT, Alignment.TOP_CENTER, Alignment.TOP_RIGHT),
        y, button.height)
    keyboardButtons.add(button)
    return this
  }

  fun act(delta:Float) {
    updateState()
  }

  private fun updateState() {
    isDown = checkIfButtonIsDown()

    isClicked = isDown && !wasDown
    wasDown = isDown
  }

  private fun checkIfButtonIsDown(): Boolean {
    for(button in keyboardButtons) {
      if( button.isDown ) {
        return true
      }
    }

    return false
  }
}

fun PlusMinus(plus:Boolean, minus:Boolean) : Int {
  if( plus && !minus) return 1
  else if (minus && !plus ) return -1
  else return 0
}

class ButtonList {
  fun newButton() : Button {
    val button = Button()
    buttons.add(button)
    return button
  }

  fun act(delta:Float) {
    for(button in buttons) {
      button.act(delta)
    }
  }

  private val buttons = Array<Button>()
}

abstract class BaseGameControls {
}
