package io.github.AISurvivors.view.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {
    private static final float PPM = 32f;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final TiledMap map;
    private final OrthogonalTiledMapRenderer mapRenderer;

    public GameScreen() {
        map = new TmxMapLoader().load("maps/warzone_ai_frontline.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1f / PPM);

        MapProperties p = map.getProperties();
        float mapWidth = ((Integer) p.get("width")) * ((Integer) p.get("tilewidth")) / PPM;
        float mapHeight = ((Integer) p.get("height")) * ((Integer) p.get("tileheight")) / PPM;

        viewport = new FitViewport(mapWidth, mapHeight, camera);
        camera.position.set(mapWidth * 0.5f, mapHeight * 0.5f, 0f);
        camera.update();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.08f, 0.08f, 0.1f, 1f);
        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();
    }

    @Override
    public void dispose() {
        mapRenderer.dispose();
        map.dispose();
    }
}
