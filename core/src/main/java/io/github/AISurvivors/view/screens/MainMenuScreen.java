package io.github.AISurvivors.view.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * Tela inicial (menu principal) do IA SURVIVOR.
 *
 * <p>Tema apocaliptico de IA: fundo escuro com vinheta avermelhada, scanlines de
 * "terminal", brilho pulsante atras do titulo e dois botoes (JOGAR e
 * CONFIGURACOES). Desenhada de forma manual (ShapeRenderer + BitmapFont), no
 * mesmo estilo do HUD do jogo, com deteccao de clique/hover propria.</p>
 */
public class MainMenuScreen implements Screen {
    private final Game game;
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 uiProjection = new Matrix4();

    private final Rectangle playButton = new Rectangle();
    private final Rectangle configButton = new Rectangle();

    private float time;

    private static final Color BG_TOP = new Color(0.04f, 0.04f, 0.06f, 1f);
    private static final Color BG_BOTTOM = new Color(0.12f, 0.045f, 0.04f, 1f);
    private static final Color AMBER = new Color(1f, 0.72f, 0.18f, 1f);
    private static final Color DANGER = new Color(0.90f, 0.28f, 0.22f, 1f);

    public MainMenuScreen(Game game) {
        this.game = game;
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont(
            Gdx.files.internal("ui/dialog_font.fnt"),
            Gdx.files.internal("ui/dialog_font.png"),
            false
        );
        font.setUseIntegerPositions(false);
    }

    private void layout(float w, float h) {
        float buttonWidth = Math.min(380f, w * 0.5f);
        float buttonHeight = 66f;
        float gap = 24f;
        float centerX = w * 0.5f;
        float topButtonY = h * 0.40f;
        playButton.set(centerX - buttonWidth * 0.5f, topButtonY, buttonWidth, buttonHeight);
        configButton.set(centerX - buttonWidth * 0.5f, topButtonY - buttonHeight - gap, buttonWidth, buttonHeight);
    }

    private boolean hovering(Rectangle r, float mouseX, float mouseY) {
        return r.contains(mouseX, mouseY);
    }

    @Override
    public void render(float delta) {
        time += delta;
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        layout(w, h);

        float mouseX = Gdx.input.getX();
        float mouseY = h - Gdx.input.getY();   // converte para y de baixo para cima
        boolean playHover = hovering(playButton, mouseX, mouseY);
        boolean configHover = hovering(configButton, mouseX, mouseY);

        uiProjection.setToOrtho2D(0f, 0f, w, h);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(uiProjection);
        shapeRenderer.begin(ShapeType.Filled);

        // fundo em degrade (topo escuro -> base avermelhada)
        shapeRenderer.rect(0f, 0f, w, h, BG_BOTTOM, BG_BOTTOM, BG_TOP, BG_TOP);

        // marcos verticais (em pixels a partir do topo) para nao sobrepor textos
        float titleTopY = h - 110f;      // topo do titulo
        float subtitleCenterY = h - 205f; // centro do subtitulo
        float accentTopY = h - 96f;
        float accentBottomY = h - 235f;
        float glowY = h - 150f;

        // brilho pulsante vermelho atras do titulo
        float pulse = 0.12f + 0.06f * MathUtils.sin(time * 2.2f);
        for (int i = 3; i >= 1; i--) {
            shapeRenderer.setColor(0.8f, 0.15f, 0.10f, pulse / i);
            shapeRenderer.ellipse(w * 0.5f - 260f * i * 0.6f, glowY - 90f * i * 0.6f,
                520f * i * 0.6f, 180f * i * 0.6f);
        }

        // linhas de acento (amarelo) emoldurando o titulo/subtitulo
        shapeRenderer.setColor(AMBER.r, AMBER.g, AMBER.b, 0.9f);
        shapeRenderer.rect(w * 0.5f - 300f, accentTopY, 600f, 3f);
        shapeRenderer.rect(w * 0.5f - 230f, accentBottomY, 460f, 3f);

        // botoes
        drawButton(playButton, playHover, 0.55f, 0.30f, 0.05f, 0.85f, 0.48f, 0.10f,
            AMBER);
        drawButton(configButton, configHover, 0.14f, 0.16f, 0.22f, 0.22f, 0.26f, 0.34f,
            new Color(0.45f, 0.6f, 0.78f, 1f));

        shapeRenderer.end();

        // scanlines (efeito de terminal/IA)
        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.10f);
        for (float y = 0; y < h; y += 3f) {
            shapeRenderer.rect(0f, y, w, 1f);
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // textos
        batch.setProjectionMatrix(uiProjection);
        batch.begin();
        drawTitle("IA SURVIVOR", 2.6f, w * 0.5f, titleTopY);
        drawCentered("// PROTOCOLO DE EXTINCAO", 1.0f, DANGER, w * 0.5f, subtitleCenterY);
        drawCentered("JOGAR", 1.25f, Color.WHITE, playButton.x + playButton.width * 0.5f,
            playButton.y + playButton.height * 0.5f);
        drawCentered("CONFIGURACOES", 1.1f, new Color(0.85f, 0.92f, 1f, 1f),
            configButton.x + configButton.width * 0.5f, configButton.y + configButton.height * 0.5f);
        drawCentered("Sobreviva a singularidade. Enter para jogar.", 0.8f,
            new Color(0.55f, 0.6f, 0.58f, 1f), w * 0.5f, 44f);
        batch.end();

        handleInput(playHover, configHover);
    }

    private void drawButton(Rectangle r, boolean hover,
                            float fr, float fg, float fb,
                            float hr, float hg, float hb,
                            Color border) {
        // moldura
        shapeRenderer.setColor(border.r, border.g, border.b, hover ? 1f : 0.8f);
        shapeRenderer.rect(r.x - 2f, r.y - 2f, r.width + 4f, r.height + 4f);
        // preenchimento
        if (hover) {
            shapeRenderer.setColor(hr, hg, hb, 1f);
        } else {
            shapeRenderer.setColor(fr, fg, fb, 1f);
        }
        shapeRenderer.rect(r.x, r.y, r.width, r.height);
        // brilho superior
        shapeRenderer.setColor(1f, 1f, 1f, hover ? 0.16f : 0.07f);
        shapeRenderer.rect(r.x, r.y + r.height * 0.6f, r.width, r.height * 0.32f);
    }

    private void drawTitle(String text, float scale, float centerX, float y) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = centerX - layout.width * 0.5f;
        // sombra
        font.setColor(0.25f, 0.02f, 0.02f, 1f);
        font.draw(batch, layout, x + 4f, y - 4f);
        // texto principal
        font.setColor(AMBER);
        font.draw(batch, layout, x, y);
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
    }

    private void drawCentered(String text, float scale, Color color, float centerX, float y) {
        font.getData().setScale(scale);
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, layout, centerX - layout.width * 0.5f, y + layout.height * 0.5f);
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
    }

    private void handleInput(boolean playHover, boolean configHover) {
        boolean clicked = Gdx.input.justTouched();
        if ((clicked && playHover) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new GameScreen(game));
            dispose();
        } else if (clicked && configHover) {
            game.setScreen(new ConfigScreen(game));
            dispose();
        }
    }

    @Override
    public void resize(int width, int height) {
        layout(width, height);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);  // usamos polling; evita processador pendente de outra tela
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
