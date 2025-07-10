package org.telegram.ui.Stars;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;
import static org.telegram.messenger.AndroidUtilities.lerp;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

public class StarGiftPatterns {

    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_ACTION = 1;
    public static final int TYPE_GIFT = 2;
    public static final int TYPE_LINK_PREVIEW = 3;

    private static final float[][] patternLocations = new float[][] {
        {
            83.33f, 24, 27.33f, .22f,
            68.66f, 75.33f, 25.33f, .21f,
            0, 86, 25.33f, .12f,
            -68.66f, 75.33f, 25.33f, .21f,
            -82.66f, 13.66f, 27.33f, .22f,
            -80, -33.33f, 20, .24f,
            -46.5f, -63.16f, 27, .21f,
            1, -82.66f, 20, .15f,
            46.5f, -63.16f, 27, .21f,
            80, -33.33f, 19.33f, .24f,

            115.66f, -63, 20, .15f,
            134, -10.66f, 20, .18f,
            118.66f, 55.66f, 20, .15f,
            124.33f, 98.33f, 20, .11f,

            -128, 98.33f, 20, .11f,
            -108, 55.66f, 20, .15f,
            -123.33f, -10.66f, 20, .18f,
            -116, -63.33f, 20, .15f
        },
        {
            27.33f, -57.66f, 20, .12f,
            59, -32, 19.33f, .22f,
            77, 4.33f, 22.66f, .2f,
            100, 40.33f, 18, .12f,
            58.66f, 59, 20, .18f,
            73.33f, 100.33f, 22.66f, .15f,
            75, 155, 22, .11f,

            -27.33f, -57.33f, 20, .12f,
            -59, -32.33f, 19.33f, .2f,
            -77, 4.66f, 23.33f, .2f,
            -98.66f, 41, 18.66f, .12f,
            -58, 59.33f, 19.33f, .18f,
            -73.33f, 100, 22, .15f,
            -75.66f, 155, 22, .11f
        },
        {
            -0.83f, -52.16f, 12.33f, .2f,
            26.66f, -40.33f, 16, .2f,
            44.16f, -20.5f, 12.33f, .2f,
            53, 7.33f, 16, .2f,
            31, 23.66f, 14.66f, .2f,
            0, 32, 13.33f, .2f,
            -29, 23.66f, 14, .2f,
            -53, 7.33f, 16, .2f,
            -44.5f, -20.16f, 12.33f, .2f,
            -27.33f, -40.33f, 16, .2f,
            43.66f, 50, 14.66f, .2f,
            -41.66f, 48, 14.66f, .2f
        },
        {
            -0.16f, -103.5f, 20.33f, .15f,
            39.66f, -77.33f, 26.66f, .15f,
            70.66f, -46.33f, 21.33f, .15f,
            84.5f, -3.83f, 29.66f, .15f,
            65.33f, 56.33f, 24.66f, .15f,
            0, 67.66f, 24.66f, .15f,
            -65.66f, 56.66f, 24.66f, .15f,
            -85, -4, 29.33f, .15f,
            -70.66f, -46.33f, 21.33f, .15f,
            -40.33f, -77.66f, 26.66f, .15f,

            62.66f, -109.66f, 21.33f, .11f,
            103.166f, -67.5f, 20.33f, .11f,
            110.33f, 37.66f, 20.66f, .11f,
            94.166f, 91.16f, 20.33f, .11f,
            38.83f, 91.16f, 20.33f, .11f,
            0, 112.5f, 20.33f, .11f,
            -38.83f, 91.16f, 20.33f, .11f,
            -94.166f, 91.16f, 20.33f, .11f,
            -110.33f, 37.66f, 20.66f, .11f,
            -103.166f, -67.5f, 20.33f, .11f,
            -62.66f, -109.66f, 21.33f, .11f
        }
    };

    public static void drawPattern(Canvas canvas, Drawable pattern, float w, float h, float alpha, float scale) {
        drawPattern(canvas, TYPE_DEFAULT, pattern, w, h, alpha, scale);
    }

    public static void drawPattern(Canvas canvas, int type, Drawable pattern, float w, float h, float alpha, float scale) {
        if (alpha <= 0.0f) return;
        for (int i = 0; i < patternLocations[type].length; i += 4) {
            final float x = patternLocations[type][i];
            final float y = patternLocations[type][i + 1];
            final float size = patternLocations[type][i + 2];
            final float thisAlpha = patternLocations[type][i + 3];

            float cx = x, cy = y, sz = size;
            if (w < h && type == TYPE_DEFAULT) {
                cx = y;
                cy = x;
            }
            cx *= scale;
            cy *= scale;
            sz *= scale;
            pattern.setBounds((int) (dp(cx) - dp(sz) / 2.0f), (int) (dp(cy) - dp(sz) / 2.0f), (int) (dp(cx) + dp(sz) / 2.0f), (int) (dp(cy) + dp(sz) / 2.0f));

            pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
            pattern.draw(canvas);
        }
    }

    private static final float[] profileRight = new float[] {
        -35.66f, -5, 24, .2388f,
        -14.33f, -29.33f, 20.66f, .32f,
        -15, -73.66f, 19.33f, .32f,
        -2, -99.66f, 18, .1476f,
        -64.33f, -24.66f, 23.33f, .3235f,
        -40.66f, -53.33f, 24, .3654f,
        -50.33f, -85.66f, 20, .172f,
        -96, -1.33f, 19.33f, .3343f,
        -136.66f, -13, 18.66f, .2569f,
        -104.66f, -33.66f, 20.66f, .2216f,
        -82, -62.33f, 22.66f, .2562f,
        -131.66f, -60, 18, .1316f,
        -105.66f, -88.33f, 18, .1487f
    };
    private static final float[] profileLeft = new float[] {
        0, -107.33f, 16, .1505f,
        14.33f, -84, 18, .1988f,
        0, -50.66f, 18.66f, .3225f,
        13, -15, 18.66f, .37f,
        43.33f, 1, 18.66f, .3186f
    };

    public static void drawOldProfilePattern(Canvas canvas, Drawable pattern, float w, float h, float alpha, float full) {
        if (alpha <= 0.0f) return;

        final float b = h;
        final float l = 0, r = w;

        if (full > 0) {
            for (int i = 0; i < profileLeft.length; i += 4) {
                final float x = profileLeft[i];
                final float y = profileLeft[i + 1];
                final float size = profileLeft[i + 2];
                final float thisAlpha = profileLeft[i + 3];

                pattern.setBounds(
                    (int) (l + dpf2(x) - dpf2(size) / 2.0f),
                    (int) (b + dpf2(y) - dpf2(size) / 2.0f),
                    (int) (l + dpf2(x) + dpf2(size) / 2.0f),
                    (int) (b + dpf2(y) + dpf2(size) / 2.0f)
                );
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha * full));
                pattern.draw(canvas);
            }

            final float sl = 77.5f, sr = 173.33f;
            final float space = w / AndroidUtilities.density - sl - sr;
            int count = Math.max(0, Math.round(space / 27.25f));
            if (count % 2 == 0) {
                count++;
            }
            for (int i = 0; i < count; ++i) {
                final float x = sl + space * ((float) i / (count - 1));
                final float y = i % 2 == 0 ? 0 : -12.5f;
                final float size = 17;
                final float thisAlpha = .21f;

                pattern.setBounds(
                    (int) (l + dpf2(x) - dpf2(size) / 2.0f),
                    (int) (b + dpf2(y) - dpf2(size) / 2.0f),
                    (int) (l + dpf2(x) + dpf2(size) / 2.0f),
                    (int) (b + dpf2(y) + dpf2(size) / 2.0f)
                );
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha * full));
                pattern.draw(canvas);
            }
        }

        for (int i = 0; i < profileRight.length; i += 4) {
            final float x = profileRight[i];
            final float y = profileRight[i + 1];
            final float size = profileRight[i + 2];
            final float thisAlpha = profileRight[i + 3];

            pattern.setBounds(
                (int) (r + dpf2(x) - dpf2(size) / 2.0f),
                (int) (b + dpf2(y) - dpf2(size) / 2.0f),
                (int) (r + dpf2(x) + dpf2(size) / 2.0f),
                (int) (b + dpf2(y) + dpf2(size) / 2.0f)
            );
            pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
            pattern.draw(canvas);
        }
    }

    private static final float[] profileV3Right = new float[] {
            26f, .24f, // top center
            26f, .32f, // top left middle
            26f, .32f, // top right middle
            22f, .20f, // top left
            22f, .20f, // top right
            26f, .32f, // center left middle
            22f, .20f, // center left
            26f, .32f, // bottom left center
            24f, .20f, // bottom left
            24f, .20f, // bottom center left
            26f, .20f, // bottom center
            24f, .20f, // bottom center right
            24f, .20f, // bottom right,
            26f, .32f, // bottom right center
            22f, .20f, // center right
            26f, .32f, // center right middle
            24f, .20f, // top center right
            24f, .20f, // top center left
    };

    public static void drawProfilePattern(Canvas canvas, Drawable pattern, View avatarContainer, float contentExpandProgress, float maxAvatarScale) {
        if (contentExpandProgress <= 0.0f || avatarContainer == null) return;

        final float avatarX = avatarContainer.getX();
        final float avatarY = avatarContainer.getY();
        final float avatarWidth = avatarContainer.getWidth() * avatarContainer.getScaleX();
        final float avatarHeight = avatarContainer.getHeight() * avatarContainer.getScaleY();
        final float avatarCenterX = avatarX + avatarWidth / 2.0f;
        final float avatarCenterY = avatarY + avatarHeight / 2.0f;
        final float avatarMaxWidth = avatarContainer.getWidth() * maxAvatarScale;
        final float avatarMaxHeight = avatarContainer.getHeight() * maxAvatarScale;

        float contentCollapseProgress = 1f - contentExpandProgress;
        float alpha;
        for (int i = 0; i < profileV3Right.length; i+=2) {
            float size = dpf2(profileV3Right[i]);
            float thisAlpha = profileV3Right[i + 1];
            if (i == 0) {
                // top center
                pattern.setBounds(
                        (int) (avatarCenterX - size / 2f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.8f, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.45f),
                        (int) (avatarCenterX + size / 2f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.8f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.45f)
                );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 0.25f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 2) {
                // top left middle
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 0.7f, avatarCenterX - size / 2f, contentCollapseProgress, 0.1f, 0.38f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.5f, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.38f),
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 0.7f + size, avatarCenterX + size / 2f, contentCollapseProgress, 0.1f, 0.38f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.5f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.38f)
                );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 0.23f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 4) {
                // top right middle
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 0.7f - size, avatarCenterX - size / 2f, contentCollapseProgress, 0.1f, 0.38f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.5f, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.38f),
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 0.7f, avatarCenterX + size / 2f, contentCollapseProgress, 0.1f, 0.38f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.5f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.38f)
                );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 0.23f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 6) {
                // top left
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 1.1f, avatarCenterX - size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.65f, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 1.1f + size, avatarCenterX + size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.65f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.6f)
                );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 1f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 8) {
                // top right
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 1.1f - size, avatarCenterX - size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.65f, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 1.1f, avatarCenterX + size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.65f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.6f)
                );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 1f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 10) {
                // center left middle
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth, avatarCenterX - size / 2f, contentCollapseProgress, 0.2f, 0.7f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.05f, avatarCenterY - size / 2f, contentCollapseProgress, 0.2f, 0.7f),
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth + size, avatarCenterX + size / 2f, contentCollapseProgress, 0.2f, 0.7f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.05f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.2f, 0.7f)
                );
                alpha = normalisedLerp(1f, 0.2f, contentCollapseProgress, 0.2f, 0.6f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 12) {
                // center left
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 1.5f, avatarCenterX - size / 2f, contentCollapseProgress, 0.2f, 0.8f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.05f, avatarCenterY - size / 2f, contentCollapseProgress, 0.2f, 0.8f),
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 1.5f + size, avatarCenterX + size / 2f, contentCollapseProgress, 0.2f, 0.8f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.05f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.2f, 0.8f)
                );
                alpha = normalisedLerp(1f, 0.2f, contentCollapseProgress, 0.2f, 1f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 14) {
                // bottom left
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 0.7f, avatarCenterX - size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.3f, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 0.7f + size, avatarCenterX + size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.3f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.6f)
                );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 0.4f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 16) {
                // bottom left
               pattern.setBounds(
                       (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 1.2f, avatarCenterX - size / 2f, contentCollapseProgress, 0.1f, 0.7f),
                       (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.56f, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.7f),
                       (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 1.2f + size, avatarCenterX + size / 2f, contentCollapseProgress, 0.1f, 0.7f),
                       (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.56f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.7f)
               );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 1f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
               pattern.draw(canvas);
            } else if (i == 18) {
                // bottom center left
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 0.6f, avatarCenterX - size / 2f, contentCollapseProgress, 0.2f, 0.9f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.8f, avatarCenterY - size / 2f, contentCollapseProgress, 0.2f, 0.9f),
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 0.6f + size, avatarCenterX + size / 2f, contentCollapseProgress, 0.2f, 0.9f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.8f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.2f, 0.9f)
                );
                alpha = normalisedLerp(1f, 0.3f, contentCollapseProgress, 0.2f, 1f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 20) {
                 // bottom center
                pattern.setBounds(
                        (int) (avatarCenterX - size / 2f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.8f - size, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.45f),
                        (int) (avatarCenterX + size / 2f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.8f, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.45f)
                );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 0.25f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 22) {
                // bottom center right
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 0.6f - size, avatarCenterX - size / 2f, contentCollapseProgress, 0.2f, 0.9f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.8f, avatarCenterY - size / 2f, contentCollapseProgress, 0.2f, 0.9f),
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 0.6f, avatarCenterX + size / 2f, contentCollapseProgress, 0.2f, 0.9f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.8f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.2f, 0.9f)
                );
                alpha = normalisedLerp(1f, 0.3f, contentCollapseProgress, 0.2f, 1f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 24) {
                // bottom right
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 1.2f - size, avatarCenterX - size / 2f, contentCollapseProgress, 0.1f, 0.7f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.56f, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.7f),
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 1.2f, avatarCenterX + size / 2f, contentCollapseProgress, 0.1f, 0.7f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.56f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.7f)
                );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 1f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 26) {
                // bottom right center
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 0.7f - size, avatarCenterX - size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.3f, avatarCenterY - size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 0.7f, avatarCenterX + size / 2f, contentCollapseProgress, 0.1f, 0.6f),
                        (int) normalisedLerp(avatarCenterY + avatarMaxHeight * 0.3f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.1f, 0.6f)
                );
                alpha = normalisedLerp(1f, 0f, contentCollapseProgress, 0.1f, 0.4f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 28) {
                // center right
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 1.5f - size, avatarCenterX - size / 2f, contentCollapseProgress, 0.2f, 0.8f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.05f, avatarCenterY - size / 2f, contentCollapseProgress, 0.2f, 0.8f),
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 1.5f, avatarCenterX + size / 2f, contentCollapseProgress, 0.2f, 0.8f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.05f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.2f, 0.8f)
                );
                alpha = normalisedLerp(1f, 0.2f, contentCollapseProgress, 0.2f, 1f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 30) {
                // center right middle
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth - size, avatarCenterX - size / 2f, contentCollapseProgress, 0.2f, 0.7f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.05f, avatarCenterY - size / 2f, contentCollapseProgress, 0.2f, 0.7f),
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth, avatarCenterX + size / 2f, contentCollapseProgress, 0.2f, 0.7f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.05f + size, avatarCenterY + size / 2f, contentCollapseProgress, 0.2f, 0.7f)
                );
                alpha = normalisedLerp(1f, 0.2f, contentCollapseProgress, 0.2f, 0.6f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } if (i == 32) {
                // top center right
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 0.6f - size, avatarCenterX - size / 2f, contentCollapseProgress, 0.2f, 0.6f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.8f - size, avatarCenterY - size / 2f, contentCollapseProgress, 0.2f, 0.6f),
                        (int) normalisedLerp(avatarCenterX + avatarMaxWidth * 0.6f, avatarCenterX + size / 2f, contentCollapseProgress, 0.2f, 0.6f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.8f, avatarCenterY + size / 2f, contentCollapseProgress, 0.2f, 0.6f)
                );
                alpha = normalisedLerp(1f, 0.3f, contentCollapseProgress, 0.2f, 0.6f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            } else if (i == 34) {
                // top center left
                pattern.setBounds(
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 0.6f, avatarCenterX - size / 2f, contentCollapseProgress, 0.2f, 0.6f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.8f - size, avatarCenterY - size / 2f, contentCollapseProgress, 0.2f, 0.6f),
                        (int) normalisedLerp(avatarCenterX - avatarMaxWidth * 0.6f + size, avatarCenterX + size / 2f, contentCollapseProgress, 0.2f, 0.6f),
                        (int) normalisedLerp(avatarCenterY - avatarMaxHeight * 0.8f, avatarCenterY + size / 2f, contentCollapseProgress, 0.2f, 0.6f)
                );
                alpha = normalisedLerp(1f, 0.3f, contentCollapseProgress, 0.2f, 0.6f);
                pattern.setAlpha((int) (0xFF * alpha * thisAlpha));
                pattern.draw(canvas);
            }
        }
    }

    private static float normalisedLerp(float start, float end, float progress, float from, float to) {
        if (progress < from) {
            return start;
        } else if (progress > to) {
            return end;
        } else {
            float normalized = (progress - from) / (to - from);
            return lerp(start, end, normalized);
        }
    }
}
