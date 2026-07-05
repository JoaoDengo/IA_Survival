package io.github.AISurvivors.view.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

public class SpeechBubble implements Disposable {
    private static final float TEXTBOX_WIDTH = 220f;
    private static final float PADDING_X = 14f;
    private static final float PADDING_Y = 12f;
    private static final float TAIL_HEIGHT = 8f;
    private static final float PLAYER_OFFSET_Y = 42f;

    private final Texture patchTexture;
    private final NinePatch patch;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    private static final float DEFAULT_DURATION = 3.5f;

    private String currentMessage = "Sistema ativo. Sobreviva.";
    private float visibleTimer = DEFAULT_DURATION;

    public SpeechBubble() {
        patchTexture = new Texture(Gdx.files.internal("ui/speech_bubble.png"));
        patch = new NinePatch(patchTexture, 12, 12, 12, 12);
        font = new BitmapFont(
            Gdx.files.internal("ui/dialog_font.fnt"),
            Gdx.files.internal("ui/dialog_font.png"),
            false
        );
        font.setUseIntegerPositions(false);
        font.setColor(Color.WHITE);
    }

    public void setMessage(String message) {
        show(message, DEFAULT_DURATION);
    }

    /** Mostra a mensagem por {@code seconds} segundos e depois some. */
    public void show(String message, float seconds) {
        currentMessage = message;
        visibleTimer = seconds;
    }

    /** Faz o balao contar o tempo; deve ser chamado a cada frame. */
    public void update(float delta) {
        if (visibleTimer > 0f) {
            visibleTimer -= delta;
        }
    }

    public void draw(SpriteBatch batch, float playerScreenX, float playerScreenY, float screenWidth, float screenHeight) {
        if (visibleTimer <= 0f) {
            return;
        }
        layout.setText(font, currentMessage, font.getColor(), TEXTBOX_WIDTH, Align.left, true);

        float bubbleWidth = layout.width + (PADDING_X * 2f);
        float bubbleHeight = layout.height + (PADDING_Y * 2f) + TAIL_HEIGHT;
        float bubbleX = playerScreenX - (bubbleWidth * 0.5f);
        float bubbleY = playerScreenY + PLAYER_OFFSET_Y;

        bubbleX = MathUtils.clamp(bubbleX, 8f, screenWidth - bubbleWidth - 8f);
        bubbleY = MathUtils.clamp(bubbleY, 8f, screenHeight - bubbleHeight - 8f);

        patch.draw(batch, bubbleX, bubbleY, bubbleWidth, bubbleHeight);
        font.draw(batch, layout, bubbleX + PADDING_X, bubbleY + bubbleHeight - PADDING_Y - TAIL_HEIGHT);
    }

    @Override
    public void dispose() {
        font.dispose();
        patchTexture.dispose();
    }
}
