package juloo.keyboard2;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

public final class SafeReplace {
    public static final Strategy ANDROID_EDITABLE = new Strategy() {
        @Override
        public boolean replace(InputConnection conn, int removeBefore, String newText) {
            if (conn == null) return false;
            conn.beginBatchEdit();
            conn.deleteSurroundingText(removeBefore, 0);
            conn.commitText(newText, 1);
            conn.endBatchEdit();
            return true;
        }
    };

    public static final Strategy TERMINAL = new Strategy() {
        @Override
        public boolean replace(InputConnection conn, int removeBefore, String newText) {
            if (conn == null) return false;
            conn.beginBatchEdit();
            for (int i = 0; i < removeBefore; i++) {
                conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
            conn.commitText(newText, 1);
            conn.endBatchEdit();
            return true;
        }
    };

    public interface Strategy {
        boolean replace(InputConnection inputConnection, int removeBefore, String newText);
    }

    private SafeReplace() {}

    public static Strategy forTerminal(boolean terminal) {
        return terminal ? TERMINAL : ANDROID_EDITABLE;
    }
}
