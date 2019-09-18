package uk.co.digitme.machinemonitoring;

import android.app.Activity;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.KeyEvent;

/**
 * View for the custom keyboard, using only numbers
 */

public class CustomNumpadView extends KeyboardView {

    CustomOnKeyboardActionListener keyListener;
    Keyboard kb = null;
    public CustomNumpadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        kb = new Keyboard(context,R.xml.keyboard);
    }

    public void setActionListenerActivity(Activity act){
        keyListener = new CustomOnKeyboardActionListener(act);
        this.setOnKeyboardActionListener(keyListener);
        this.setKeyboard(kb);
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    private class CustomOnKeyboardActionListener implements OnKeyboardActionListener {
        Activity owner;

        CustomOnKeyboardActionListener(Activity activity){
            owner = activity;
        }

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            long eventTime = System.currentTimeMillis();
            KeyEvent event = new KeyEvent(eventTime, eventTime,
                    KeyEvent.ACTION_DOWN, primaryCode, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE);
            owner.dispatchKeyEvent(event);
        }

        @Override
        public void onPress(int primaryCode) {
        }

        @Override
        public void onRelease(int primaryCode) {
        }

        @Override
        public void onText(CharSequence text) {
        }

        @Override
        public void swipeDown() {
        }

        @Override
        public void swipeLeft() {
        }

        @Override
        public void swipeRight() {
        }

        @Override
        public void swipeUp() {
        }

    }

}