package com.fuzz.android.listener;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Formats so that two numbers are adjacent to a delimiter.
 */
public class MonthYearFormatWatcher implements TextWatcher {
    private static final CharSequence DELIMITER = "/";
    private EditText targetView;
    private int oldEditableLength;

    public MonthYearFormatWatcher(EditText targetView) {
        this.targetView = targetView;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int count, int before) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        int len = editable.length();
        int caretPos = targetView.getSelectionStart();

        if (caretPos == 2) {
            //  Don't touch separator
            if (len > oldEditableLength) {
                //  Appended text
                targetView.setSelection(caretPos + 1);
            } else {
                //  Deleted delimiter
                editable.append(DELIMITER);
                targetView.setSelection(caretPos);
            }
        }

        oldEditableLength = len;
    }
}
