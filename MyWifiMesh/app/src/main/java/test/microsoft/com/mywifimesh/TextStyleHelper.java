package test.microsoft.com.mywifimesh;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

/**
 * Created by adminsag on 2/25/16.
 */
public class TextStyleHelper {
    public static void createSpanBold() {
    }

    public static SpannableStringBuilder appendSpanWhiteBackground(SpannableStringBuilder outputBuilder, String additionalText, int textColor) {
        int lengthBeforeAppending = outputBuilder.length();
        outputBuilder.append(additionalText);
        outputBuilder.setSpan(new ForegroundColorSpan(textColor), lengthBeforeAppending, outputBuilder.length(), 0);
        outputBuilder.setSpan(new BackgroundColorSpan(Color.WHITE), lengthBeforeAppending, outputBuilder.length(), 0);
        return outputBuilder;
    }

    public static SpannableStringBuilder createBuilderWithContent(String initialTextContent, int textColor) {
        SpannableStringBuilder outputBuilder = new SpannableStringBuilder(initialTextContent);
        outputBuilder.setSpan(new ForegroundColorSpan(textColor), 0, outputBuilder.length(), 0);
        return outputBuilder;
    }

    public static SpannableString createSpannableString(String initialTextContent, int textColor) {
        SpannableString text = new SpannableString(initialTextContent);
        text.setSpan(new ForegroundColorSpan(textColor), 0, text.length(), 0);
        return text;
    }

    public static SpannableString colorTextBoldText(SpannableString textToAlterAndReturn, int textColor) {
        textToAlterAndReturn.setSpan(new ForegroundColorSpan(textColor), 0, textToAlterAndReturn.length(), 0);
        textToAlterAndReturn.setSpan(new StyleSpan(Typeface.BOLD), 0, textToAlterAndReturn.length(), 0);
        return textToAlterAndReturn;
    }

    public static SpannableString createSpannableStringWhiteBackground(String initialTextContent, int textColor) {
        SpannableString text = new SpannableString(initialTextContent);
        text.setSpan(new ForegroundColorSpan(textColor), 0, text.length(), 0);
        text.setSpan(new BackgroundColorSpan(Color.WHITE), 0, text.length(), 0);
        return text;
    }
}
