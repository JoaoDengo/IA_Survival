package io.github.AISurvivors.view.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PointMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.AISurvivors.controller.input.PlayerInputProcessor;
import io.github.AISurvivors.model.entity.Player;
import io.github.AISurvivors.view.assets.GameAssets;
import io.github.AISurvivors.view.camera.WorldCameraController;

public class GameScreen extends ScreenAdapter {
    private static final float PPM = 32f;
    private static final float CAMERA_WORLD_WIDTH = 28f;
    private static final float CAMERA_WORLD_HEIGHT = 15.75f;

    private static final int[] BOTTOM_LAYERS = {0, 1, 2, 3, 4, 5};
    private static final int[] TOP_LAYERS = {6};

    private final GameAssets assets;
    private final TiledMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final WorldCameraController cameraController;
    private final SpriteBatch worldBatch;
    private final SpriteBatch hudBatch;
    private final BitmapFont hudFont;
    private final PlayerInputProcessor playerInput;
    private final Player player;
    private final Music backgroundMusic;

    private final float mapWidth;
    private final float mapHeight;
    private final Vector2 inputDirection;
    private final Vector2 zoomScreenPosition;
    private final Vector2 dragPreviousScreenPosition;
    private final Vector2 dragCurrentScreenPosition;
    private final Vector2 playerScreenPosition;
    private final Matrix4 hudProjection = new Matrix4();

    public GameScreen() {
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
        hudFont = new BitmapFont();
        hudFont.setUseIntegerPositions(false);
        playerInput = new PlayerInputProcessor();
        player = new Player(
            assets.playerIdleTexture(),
            assets.playerWalkTexture(),
            assets.playerStepSound(),
            findPlayerSpawn()
        );
        backgroundMusic = assets.backgroundMusic();
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.12f);
        inputDirection = new Vector2();
        zoomScreenPosition = new Vector2();
        dragPreviousScreenPosition = new Vector2();
        dragCurrentScreenPosition = new Vector2();
        playerScreenPosition = new Vector2();
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

    private void updateCameraFromInput() {
        if (playerInput.consumeCameraReset()) {
            cameraController.resetOffset();
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
        hudBatch.setProjectionMatrix(hudProjection);

        hudBatch.begin();
        hudFont.draw(hudBatch, "WASD/setas movem o player", 20f, Gdx.graphics.getHeight() - 20f);
        hudFont.draw(hudBatch, "Mouse direito arrasta a camera", 20f, Gdx.graphics.getHeight() - 45f);
        hudFont.draw(hudBatch, "Scroll aplica zoom no cursor", 20f, Gdx.graphics.getHeight() - 70f);
        hudFont.draw(hudBatch, "Espaco recentra a camera", 20f, Gdx.graphics.getHeight() - 95f);
        hudFont.draw(hudBatch, "Player", playerScreenPosition.x - 18f, playerScreenPosition.y + 36f);
        hudBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        cameraController.resize(width, height);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(playerInput);
        if (!backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.08f, 0.08f, 0.1f, 1f);

        playerInput.getMovementDirection(inputDirection);
        boolean isMoving = !inputDirection.isZero();
        player.update(delta, inputDirection, mapWidth, mapHeight);

        cameraController.setFocusTarget(player.getPosition());
        updateCameraFromInput();
        cameraController.update();

        mapRenderer.setView(cameraController.getCamera());
        mapRenderer.render(BOTTOM_LAYERS);

        worldBatch.setProjectionMatrix(cameraController.getCamera().combined);
        worldBatch.begin();
        player.draw(worldBatch, isMoving);
        worldBatch.end();

        mapRenderer.render(TOP_LAYERS);
        renderHud();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
        backgroundMusic.pause();
    }

    @Override
    public void dispose() {
        hudFont.dispose();
        hudBatch.dispose();
        worldBatch.dispose();
        mapRenderer.dispose();
        assets.dispose();
    }
}
