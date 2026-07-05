package io.github.AISurvivors.view.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PointMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.AISurvivors.controller.input.PlayerInputProcessor;
import io.github.AISurvivors.model.collision.CollisionWorld;
import io.github.AISurvivors.model.combat.WeaponSystem;
import io.github.AISurvivors.model.fx.ParticleSystem;
import io.github.AISurvivors.model.entity.Player;
import io.github.AISurvivors.model.spawn.EnemyManager;
import io.github.AISurvivors.model.state.GameSettings;
import io.github.AISurvivors.model.state.PlayerProgression;
import io.github.AISurvivors.view.assets.GameAssets;
import io.github.AISurvivors.view.camera.WorldCameraController;
import io.github.AISurvivors.view.ui.SpeechBubble;

public class GameScreen extends ScreenAdapter {
    private final Game game;
    private final GameSettings settings;
    private final GameAssets assets;
    private final TiledMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final WorldCameraController cameraController;
    private final SpriteBatch worldBatch;
    private final SpriteBatch hudBatch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont hudFont;
    private final SpeechBubble speechBubble;
    private final PlayerInputProcessor playerInput;
    private final Player player;
    private final EnemyManager enemyManager;
    private final WeaponSystem weaponSystem;
    private final PlayerProgression progression;
    private final CollisionWorld collisionWorld;
    private final ParticleSystem particleSystem;
    private final Music backgroundMusic;
    private int lastKillCount;

    private static final float PPM = 32f;
    private static final float CAMERA_WORLD_WIDTH = 28f;
    private static final float CAMERA_WORLD_HEIGHT = 15.75f;
    private static final float ENEMY_CONTACT_DAMAGE = 10f;
    private static final int XP_PER_KILL = 10;
    private static final float DAMAGE_PER_LEVEL = 0.5f;

    // barra de XP (centralizada e afastada do topo)
    private static final float XP_BAR_MARGIN_TOP = 22f;
    private static final float XP_BAR_HEIGHT = 22f;
    private static final float XP_BAR_WIDTH_FRAC = 0.5f;
    private static final float XP_BAR_MIN_WIDTH = 340f;
    private static final Color XP_FILL_TOP = new Color(0.55f, 0.9f, 1f, 1f);
    private static final Color XP_FILL_BOTTOM = new Color(0.12f, 0.45f, 0.78f, 1f);

    // camadas do mapa (cidade em ruinas): ground, overlay(faixas/calcadas), decals, obstacles
    private static final int[] BOTTOM_LAYERS = {0, 1, 2, 3};

    private final float mapWidth;
    private final float mapHeight;
    private final Vector2 inputDirection;
    private final Vector2 zoomScreenPosition;
    private final Vector2 dragPreviousScreenPosition;
    private final Vector2 dragCurrentScreenPosition;
    private final Vector2 playerScreenPosition;
    private final Rectangle playerBounds;
    private final Matrix4 hudProjection = new Matrix4();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private boolean debugHitboxes;

    public GameScreen(Game game) {
        this.game = game;
        settings = GameSettings.load();
        assets = new GameAssets();
        assets.load();
        map = assets.map();
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f / PPM);

        MapProperties p = map.getProperties();
        mapWidth = ((Integer) p.get("width")) * ((Integer) p.get("tilewidth")) / PPM;
        mapHeight = ((Integer) p.get("height")) * ((Integer) p.get("tileheight")) / PPM;

        cameraController = new WorldCameraController(CAMERA_WORLD_WIDTH, CAMERA_WORLD_HEIGHT, mapWidth, mapHeight);
        worldBatch = new SpriteBatch();
        hudBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        hudFont = new BitmapFont();
        hudFont.setUseIntegerPositions(false);
        speechBubble = new SpeechBubble();
        speechBubble.setMessage("Sistema ativo, " + settings.playerName + ". Dificuldade: " + settings.difficulty + ".");
        playerInput = new PlayerInputProcessor();
        player = new Player(
            assets.playerIdleTexture(),
            assets.playerWalkTexture(),
            assets.playerStepSound(),
            findPlayerSpawn()
        );
        enemyManager = new EnemyManager(
            assets.enemyRobotIdleTexture(),
            assets.enemyRobotWalkTexture(),
            mapWidth,
            mapHeight,
            settings.getEnemySpawnMultiplier()
        );
        weaponSystem = new WeaponSystem();
        progression = new PlayerProgression();
        collisionWorld = buildCollisionWorld();
        particleSystem = new ParticleSystem();
        // ao morrer, o inimigo dispara uma explosao de particulas na sua posicao
        enemyManager.setDeathListener(particleSystem::burst);
        backgroundMusic = assets.backgroundMusic();
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(settings.musicVolume / 100f);
        inputDirection = new Vector2();
        zoomScreenPosition = new Vector2();
        dragPreviousScreenPosition = new Vector2();
        dragCurrentScreenPosition = new Vector2();
        playerScreenPosition = new Vector2();
        playerBounds = new Rectangle();
        Gdx.input.setInputProcessor(playerInput);

        cameraController.setFocusTarget(player.getPosition());
        cameraController.update();
    }

    private Vector2 findPlayerSpawn() {
        MapLayer spawnsLayer = map.getLayers().get("spawns");
        if (spawnsLayer == null) {
            return new Vector2(mapWidth * 0.5f, mapHeight * 0.5f);
        }

        MapObject spawnObject = spawnsLayer.getObjects().get("player_spawn");
        if (spawnObject instanceof PointMapObject point) {
            return new Vector2(point.getPoint().x / PPM, point.getPoint().y / PPM);
        }

        return new Vector2(mapWidth * 0.5f, mapHeight * 0.5f);
    }

    /**
     * Le a camada de objetos "collisions" do mapa e converte cada retangulo
     * (em pixels) para unidades do mundo. O TmxMapLoader ja entrega as
     * coordenadas dos objetos no eixo Y de baixo para cima, alinhadas com o
     * desenho dos tiles, entao basta dividir por PPM.
     */
    private CollisionWorld buildCollisionWorld() {
        CollisionWorld world = new CollisionWorld();
        MapLayer layer = map.getLayers().get("collisions");
        if (layer == null) {
            return world;
        }

        for (MapObject object : layer.getObjects()) {
            if (object instanceof RectangleMapObject rectangleObject) {
                Rectangle r = rectangleObject.getRectangle();
                world.addSolid(new Rectangle(r.x / PPM, r.y / PPM, r.width / PPM, r.height / PPM));
            }
        }

        return world;
    }

    private void updateCameraFromInput() {
        if (playerInput.consumeCameraReset()) {
            cameraController.resetOffset();
        }

        if (playerInput.consumeDebugHitboxToggle()) {
            debugHitboxes = !debugHitboxes;
        }

        if (playerInput.consumeCameraDrag(dragPreviousScreenPosition, dragCurrentScreenPosition)) {
            cameraController.panByScreenDrag(
                dragPreviousScreenPosition.x,
                dragPreviousScreenPosition.y,
                dragCurrentScreenPosition.x,
                dragCurrentScreenPosition.y
            );
        }

        float zoomAmount = playerInput.consumeZoomAmount(zoomScreenPosition);
        if (zoomAmount != 0f) {
            cameraController.zoomAt(zoomScreenPosition.x, zoomScreenPosition.y, zoomAmount);
        }
    }

    private void renderHud() {
        cameraController.worldToScreen(player.getPosition(), playerScreenPosition);
        hudProjection.setToOrtho2D(0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // barras primeiro (ShapeRenderer); os textos vao por cima (SpriteBatch)
        renderXpBar();
        renderHealthBar();

        hudBatch.setProjectionMatrix(hudProjection);

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        hudBatch.begin();
        // deixa 24px no topo livres para a barra de XP
        hudFont.draw(hudBatch, "WASD/setas movem o player", 20f, screenHeight - 44f);
        hudFont.draw(hudBatch, "Mouse direito arrasta a camera", 20f, screenHeight - 69f);
        hudFont.draw(hudBatch, "Scroll aplica zoom no cursor", 20f, screenHeight - 94f);
        hudFont.draw(hudBatch, "Espaco recentra a camera", 20f, screenHeight - 119f);
        hudFont.draw(hudBatch, "H alterna hitboxes: " + (debugHitboxes ? "ON" : "OFF"), 20f, screenHeight - 144f);
        hudFont.draw(
            hudBatch,
            "Tempo: " + (int) enemyManager.getElapsedTime() + "s | Inimigos: "
                + enemyManager.getActiveCount() + "/" + enemyManager.getActiveLimit()
                + " | Pool livre: " + enemyManager.getFreeCount(),
            20f,
            screenHeight - 169f
        );
        hudFont.draw(
            hudBatch,
            "Nivel: " + progression.getLevel() + " | Abates: " + enemyManager.getKillCount()
                + " | Balas ativas: " + weaponSystem.getActiveCount(),
            20f,
            screenHeight - 194f
        );
        hudFont.draw(
            hudBatch,
            "Perfil: " + settings.playerName + " | Dificuldade: " + settings.difficulty
                + " | Musica: " + (settings.musicEnabled ? (int) settings.musicVolume + "%" : "OFF"),
            20f,
            screenHeight - 219f
        );
        speechBubble.draw(hudBatch, playerScreenPosition.x, playerScreenPosition.y, screenWidth, screenHeight);
        hudFont.draw(hudBatch, "Vida: " + (int) player.getHealth() + "/" + (int) player.getMaxHealth(), 20f, 52f);
        // rotulo do nivel centralizado sobre a barra de XP
        drawXpBarLabel("Nivel " + progression.getLevel(), screenWidth);
        hudBatch.end();
    }

    private void renderXpBar() {
        float bx = xpBarX();
        float by = xpBarY();
        float bw = xpBarWidth();
        float bh = XP_BAR_HEIGHT;
        float xpPercent = MathUtils.clamp(progression.getXpPercent(), 0f, 1f);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(hudProjection);
        shapeRenderer.begin(ShapeType.Filled);

        // moldura externa escura
        float f = 2f;
        shapeRenderer.setColor(0.03f, 0.04f, 0.06f, 0.92f);
        shapeRenderer.rect(bx - f, by - f, bw + 2f * f, bh + 2f * f);

        // trilho interno (vazio)
        shapeRenderer.setColor(0.13f, 0.15f, 0.20f, 0.95f);
        shapeRenderer.rect(bx, by, bw, bh);

        // preenchimento em degrade vertical (aspecto envidracado)
        float fillW = bw * xpPercent;
        if (fillW > 0f) {
            shapeRenderer.rect(bx, by, fillW, bh,
                XP_FILL_BOTTOM, XP_FILL_BOTTOM, XP_FILL_TOP, XP_FILL_TOP);
            // brilho no topo do preenchimento
            shapeRenderer.setColor(1f, 1f, 1f, 0.28f);
            shapeRenderer.rect(bx, by + bh * 0.62f, fillW, bh * 0.28f);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private float xpBarWidth() {
        return Math.max(XP_BAR_MIN_WIDTH, Gdx.graphics.getWidth() * XP_BAR_WIDTH_FRAC);
    }

    private float xpBarX() {
        return (Gdx.graphics.getWidth() - xpBarWidth()) * 0.5f;
    }

    private float xpBarY() {
        return Gdx.graphics.getHeight() - XP_BAR_MARGIN_TOP - XP_BAR_HEIGHT;
    }

    private void renderHealthBar() {
        float barX = 20f;
        float barY = 20f;
        float barWidth = 260f;
        float barHeight = 18f;
        float healthPercent = player.getHealth() / player.getMaxHealth();
        if (healthPercent < 0f) {
            healthPercent = 0f;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(hudProjection);
        shapeRenderer.begin(ShapeType.Filled);

        // fundo escuro da barra
        shapeRenderer.setColor(0.12f, 0.12f, 0.15f, 0.85f);
        shapeRenderer.rect(barX, barY, barWidth, barHeight);

        // preenchimento: verde (cheio) -> amarelo -> vermelho (baixo)
        float red = Math.min(1f, 2f * (1f - healthPercent));
        float green = Math.min(1f, 2f * healthPercent);
        shapeRenderer.setColor(red, green, 0.15f, 1f);
        shapeRenderer.rect(barX, barY, barWidth * healthPercent, barHeight);

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderBulletsAndParticles() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(cameraController.getCamera().combined);
        shapeRenderer.begin(ShapeType.Filled);
        particleSystem.draw(shapeRenderer);
        weaponSystem.draw(shapeRenderer);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderDebugHitboxes() {
        if (!debugHitboxes) {
            return;
        }

        player.getBounds(playerBounds);
        shapeRenderer.setProjectionMatrix(cameraController.getCamera().combined);
        shapeRenderer.begin(ShapeType.Line);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(playerBounds.x, playerBounds.y, playerBounds.width, playerBounds.height);
        shapeRenderer.setColor(Color.ORANGE);
        enemyManager.drawDebug(shapeRenderer);
        shapeRenderer.setColor(Color.CYAN);
        weaponSystem.drawDebug(shapeRenderer);
        shapeRenderer.setColor(Color.LIME);
        collisionWorld.drawDebug(shapeRenderer);
        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        cameraController.resize(width, height);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(playerInput);
        if (settings.musicEnabled && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.08f, 0.08f, 0.1f, 1f);

        boolean playerAlive = player.isAlive();

        // enquanto o player estiver vivo o mundo evolui; morto, tudo congela
        if (playerAlive) {
            playerInput.getMovementDirection(inputDirection);
            player.update(delta, inputDirection, mapWidth, mapHeight, collisionWorld);

            cameraController.setFocusTarget(player.getPosition());
            updateCameraFromInput();
            cameraController.update();

            float visibleWidth = cameraController.getViewport().getWorldWidth() * cameraController.getCamera().zoom;
            float visibleHeight = cameraController.getViewport().getWorldHeight() * cameraController.getCamera().zoom;
            enemyManager.update(delta, player.getPosition(), visibleWidth, visibleHeight, collisionWorld);
            weaponSystem.update(delta, player.getPosition(), enemyManager);
            particleSystem.update(delta);
            speechBubble.update(delta);

            // dano por contato: inimigo encostou no player => tira vida (com i-frames)
            player.getBounds(playerBounds);
            if (enemyManager.isTouchingPlayer(playerBounds)) {
                player.takeDamage(ENEMY_CONTACT_DAMAGE);
            }

            // cada novo abate vira XP; XP suficiente => sobe de nivel e ganha dano
            int kills = enemyManager.getKillCount();
            if (kills > lastKillCount) {
                int levelsGained = progression.addXp((kills - lastKillCount) * XP_PER_KILL);
                lastKillCount = kills;
                if (levelsGained > 0) {
                    weaponSystem.increaseDamage(levelsGained * DAMAGE_PER_LEVEL);
                    // inimigos que nascerem a partir de agora ficam mais resistentes
                    enemyManager.scaleToPlayerLevel(progression.getLevel());
                    // balao aparece e some rapidamente ao subir de nivel
                    speechBubble.show("Nivel " + progression.getLevel() + "! Dano: "
                        + String.format("%.1f", weaponSystem.getBulletDamage())
                        + " | Vida inimigo: " + (int) enemyManager.getSpawnHealth(), 1.2f);
                }
            }
        }

        mapRenderer.setView(cameraController.getCamera());
        mapRenderer.render(BOTTOM_LAYERS);

        worldBatch.setProjectionMatrix(cameraController.getCamera().combined);
        worldBatch.begin();
        enemyManager.draw(worldBatch);
        player.draw(worldBatch);
        worldBatch.end();

        renderBulletsAndParticles();

        renderDebugHitboxes();
        renderHud();

        if (!playerAlive) {
            renderGameOver();
            handleGameOverInput();
        }
    }

    private void renderGameOver() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // escurece a tela inteira
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(hudProjection);
        shapeRenderer.begin(ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.72f);
        shapeRenderer.rect(0f, 0f, screenWidth, screenHeight);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        hudBatch.setProjectionMatrix(hudProjection);
        hudBatch.begin();

        drawCenteredText("GAME OVER", 2.4f, new Color(1f, 0.35f, 0.3f, 1f),
            screenWidth, screenHeight * 0.5f + 70f);
        drawCenteredText(
            "Voce sobreviveu " + (int) enemyManager.getElapsedTime() + "s  |  Abates: " + enemyManager.getKillCount(),
            1.1f, Color.WHITE, screenWidth, screenHeight * 0.5f + 8f);
        drawCenteredText("R - reiniciar        ESC - menu", 1.2f,
            new Color(0.8f, 0.9f, 1f, 1f), screenWidth, screenHeight * 0.5f - 48f);

        hudBatch.end();
    }

    private void drawXpBarLabel(String text, float screenWidth) {
        hudFont.getData().setScale(0.9f);
        hudFont.setColor(Color.WHITE);
        glyphLayout.setText(hudFont, text);
        float x = (screenWidth - glyphLayout.width) * 0.5f;
        float y = xpBarY() + (XP_BAR_HEIGHT + glyphLayout.height) * 0.5f;  // centralizado na barra
        hudFont.draw(hudBatch, glyphLayout, x, y);
        hudFont.getData().setScale(1f);
    }

    private void drawCenteredText(String text, float scale, Color color, float screenWidth, float y) {
        hudFont.getData().setScale(scale);
        hudFont.setColor(color);
        glyphLayout.setText(hudFont, text);
        hudFont.draw(hudBatch, glyphLayout, (screenWidth - glyphLayout.width) * 0.5f, y);
        hudFont.setColor(Color.WHITE);
        hudFont.getData().setScale(1f);
    }

    private void handleGameOverInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            game.setScreen(new GameScreen(game));
            dispose();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
        }
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        backgroundMusic.pause();
    }

    @Override
    public void dispose() {
        particleSystem.dispose();
        weaponSystem.dispose();
        enemyManager.dispose();
        speechBubble.dispose();
        hudFont.dispose();
        shapeRenderer.dispose();
        hudBatch.dispose();
        worldBatch.dispose();
        mapRenderer.dispose();
        assets.dispose();
    }
}
