package com.madeso.spaceman

import com.badlogic.gdx.utils.Disposable
import java.util.ArrayList

class Destructor : Disposable {
  internal var disposables = ArrayList<Disposable>()

  fun add(d: Disposable): Destructor {
    disposables.add(d)
    return this
  }

  override fun dispose() {
    for (d in disposables) {
      d.dispose()
    }
  }
}
