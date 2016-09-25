package com.madeso.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Array

class Button {
  var isDown = false
    private set

  private var wasDown = false

  var isClicked = false
    private set

  private val keyboardButtons = Array<Int>()

  fun addKeyboard(button:Int) : Button {
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
      if( Gdx.input.isKeyPressed(button) ) {
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
