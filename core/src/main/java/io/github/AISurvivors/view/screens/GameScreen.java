package io.github.AISurvivors.view.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PointMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.AISurvivors.controller.input.PlayerInputProcessor;

public class GameScreen extends ScreenAdapter {
    private static final float PPM = 32f;
    private static final float CAMERA_WORLD_WIDTH = 28f;
    private static final float CAMERA_WORLD_HEIGHT = 15.75f;
    private static final float PLAYER_WORLD_SIZE = 1.8f;
    private static final float PLAYER_SPEED = 8f;

    private static final int[] BOTTOM_LAYERS = {0, 1, 2, 3, 4, 5};
    private static final int[] TOP_LAYERS = {6};

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final TiledMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;
    private final SpriteBatch playerBatch;
    private final Texture playerWalkTexture;
    private final Texture playerIdleTexture;
    private final TextureRegion playerIdleRegion;
    private final Animation<TextureRegion> playerWalkAnimation;
    private final PlayerInputProcessor playerInput;

    private final float mapWidth;
    private final float mapHeight;
    private final Vector2 playerPosition;
    private final Vector2 inputDirection;
    private float playerAnimTime;

    public GameScreen() {
        map = new TmxMapLoader().load("maps/warzone_ai_frontline.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f / PPM);

        MapProperties p = map.getProperties();
        mapWidth = ((Integer) p.get("width")) * ((Integer) p.get("tilewidth")) / PPM;
        mapHeight = ((Integer) p.get("height")) * ((Integer) p.get("tileheight")) / PPM;

        viewport = new FitViewport(CAMERA_WORLD_WIDTH, CAMERA_WORLD_HEIGHT, camera);

        playerWalkTexture = new Texture("sprites/player/walk/player_apocalyptic_walk_sheet.png");
        playerWalkTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        playerIdleTexture = new Texture("sprites/player/player_apocalyptic_idle.png");
        playerIdleTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        playerIdleRegion = new TextureRegion(playerIdleTexture);

        TextureRegion[][] split = TextureRegion.split(playerWalkTexture, 32, 32);
        playerWalkAnimation = new Animation<>(0.12f, split[0]);
        playerWalkAnimation.setPlayMode(Animation.PlayMode.LOOP);

        playerBatch = new SpriteBatch();
        playerInput = new PlayerInputProcessor();
        playerPosition = findPlayerSpawn();
        inputDirection = new Vector2();
        Gdx.input.setInputProcessor(playerInput);

        updateCamera();
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

    private void updateCamera() {
        float halfW = viewport.getWorldWidth() * 0.5f;
        float halfH = viewport.getWorldHeight() * 0.5f;

        float cameraX = playerPosition.x;
        float cameraY = playerPosition.y;

        if (mapWidth > viewport.getWorldWidth()) {
            cameraX = MathUtils.clamp(playerPosition.x, halfW, mapWidth - halfW);
        }

        if (mapHeight > viewport.getWorldHeight()) {
            cameraY = MathUtils.clamp(playerPosition.y, halfH, mapHeight - halfH);
        }

        camera.position.set(cameraX, cameraY, 0f);
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        updateCamera();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(playerInput);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.08f, 0.08f, 0.1f, 1f);

        playerInput.getMovementDirection(inputDirection);
        boolean isMoving = !inputDirection.isZero();

        if (isMoving) {
            float halfPlayerSize = PLAYER_WORLD_SIZE * 0.5f;
            playerPosition.x = MathUtils.clamp(
                playerPosition.x + (inputDirection.x * PLAYER_SPEED * delta),
                halfPlayerSize,
                mapWidth - halfPlayerSize
            );
            playerPosition.y = MathUtils.clamp(
                playerPosition.y + (inputDirection.y * PLAYER_SPEED * delta),
                halfPlayerSize,
                mapHeight - halfPlayerSize
            );
            playerAnimTime += delta;
        }

        updateCamera();

        mapRenderer.setView(camera);
        mapRenderer.render(BOTTOM_LAYERS);

        TextureRegion currentPlayerFrame = isMoving
            ? playerWalkAnimation.getKeyFrame(playerAnimTime, true)
            : playerIdleRegion;
        float drawX = playerPosition.x - (PLAYER_WORLD_SIZE * 0.5f);
        float drawY = playerPosition.y - (PLAYER_WORLD_SIZE * 0.5f);

        playerBatch.setProjectionMatrix(camera.combined);
        playerBatch.begin();
        playerBatch.draw(currentPlayerFrame, drawX, drawY, PLAYER_WORLD_SIZE, PLAYER_WORLD_SIZE);
        playerBatch.end();

        mapRenderer.render(TOP_LAYERS);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        playerBatch.dispose();
        playerWalkTexture.dispose();
        playerIdleTexture.dispose();
        mapRenderer.dispose();
        map.dispose();
    }
}
