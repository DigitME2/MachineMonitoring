package uk.co.digitme.machinemonitoring.Helpers;

import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class NextKeyListener implements View.OnKeyListener {

    /**
     * A key listener for the "next button". If an edit text is passed to the constructor,
     * that edit text is focussed when the next button is pushed.
     * <p>
     * If a button is passed then it is clicked when the next button is pushed. The on key listener
     * is then removed from the button, to stop double presses
     */

    private EditText nextET;
    private Button sendButton;

    public NextKeyListener(EditText et) {
        this.nextET = et;
    }

    public NextKeyListener(Button button) {
        this.sendButton = button;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == 66) {
            if (sendButton == null) {
                nextET.requestFocus();

            } else {
                sendButton.performClick();
                sendButton.setOnKeyListener(null);
            }
        }
        return false;
    }
}
