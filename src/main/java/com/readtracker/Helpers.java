package com.readtracker;

import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

/**
 * Static android helpers
 */
public class Helpers {

  /**
   * Determine if the given action is a done action.
   * This is true if the action id is IME_ACTION_DONE or if the user
   * pressed enter on the keyboard.
   *
   * @param actionId Action id of the editor action
   * @param event    Key Event to check
   * @return true of the given event is a key down event for the enter key
   */
  public static boolean isDoneAction(int actionId, KeyEvent event) {
    boolean isActionEnter = event != null
        && event.getAction() == KeyEvent.ACTION_DOWN
        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
    return actionId == EditorInfo.IME_ACTION_DONE || isActionEnter;
  }

}
