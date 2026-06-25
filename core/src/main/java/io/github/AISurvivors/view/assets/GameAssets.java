package io.github.AISurvivors.view.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.Disposable;

public class GameAssets implements Disposable {
    public static final String MAP = "maps/warzone_ai_frontline.tmx";
    public static final String PLAYER_IDLE = "sprites/player/player_apocalyptic_idle.png";
    public static final String PLAYER_WALK = "sprites/player/walk/player_apocalyptic_walk_sheet.png";
    public static final String ENEMY_ROBOT_IDLE = "sprites/enemy/enemy_apocalyptic_robot_idle.png";
    public static final String ENEMY_ROBOT_WALK = "sprites/enemy/walk/enemy_apocalyptic_robot_walk_sheet.png";
    public static final String PLAYER_STEP_SOUND = "audio/player_step.wav";
    public static final String BACKGROUND_MUSIC = "audio/background_loop.wav";

    private final AssetManager assetManager = new AssetManager();

    public GameAssets() {
        assetManager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
    }

    public void load() {
        assetManager.load(MAP, TiledMap.class);
        assetManager.load(PLAYER_IDLE, Texture.class);
        assetManager.load(PLAYER_WALK, Texture.class);
        assetManager.load(ENEMY_ROBOT_IDLE, Texture.class);
        assetManager.load(ENEMY_ROBOT_WALK, Texture.class);
        assetManager.load(PLAYER_STEP_SOUND, Sound.class);
        assetManager.load(BACKGROUND_MUSIC, Music.class);
        assetManager.finishLoading();
        configureTextures();
    }

    private void configureTextures() {
        playerIdleTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        playerWalkTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        enemyRobotIdleTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        enemyRobotWalkTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public TiledMap map() {
        return assetManager.get(MAP, TiledMap.class);
    }

    public Texture playerIdleTexture() {
        return assetManager.get(PLAYER_IDLE, Texture.class);
    }

    public Texture playerWalkTexture() {
        return assetManager.get(PLAYER_WALK, Texture.class);
    }

    public Texture enemyRobotIdleTexture() {
        return assetManager.get(ENEMY_ROBOT_IDLE, Texture.class);
    }

    public Texture enemyRobotWalkTexture() {
        return assetManager.get(ENEMY_ROBOT_WALK, Texture.class);
    }

    public Sound playerStepSound() {
        return assetManager.get(PLAYER_STEP_SOUND, Sound.class);
    }

    public Music backgroundMusic() {
        return assetManager.get(BACKGROUND_MUSIC, Music.class);
    }

    @Override
    public void dispose() {
        assetManager.dispose();
    }
}
