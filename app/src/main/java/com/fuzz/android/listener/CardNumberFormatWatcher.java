package com.fuzz.android.listener;

import android.text.Editable;
import android.text.TextWatcher;

/**
 */
public class CardNumberFormatWatcher implements TextWatcher {
    private boolean alteringText;

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (alteringText) {
            //  Avoid recursion
            return;
        }

        int len = editable.length();

        int mod = len % 4;

        if (len > 0) {
            //  Make sure has spaces
            String formatted = editable.toString().replaceAll("\\s", "");
            int formattedLen = formatted.length();

            alteringText = true;
            editable.clear();
            for (int i = 0, n = formattedLen; i < n; i += 4) {
                editable.append(formatted.subSequence(i, Math.min(i + 4, n)));

                if (i + 4 < n) {
                    //  Delimiter
                    editable.append(' ');
                }
            }
            alteringText = false;
        }
    }
}
