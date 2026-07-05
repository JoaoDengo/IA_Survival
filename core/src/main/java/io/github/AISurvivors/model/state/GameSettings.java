package io.github.AISurvivors.model.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameSettings {
    private static final String PREFS_NAME = "aisurvivors-settings";
    private static final String KEY_PLAYER_NAME = "playerName";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_MUSIC_ENABLED = "musicEnabled";
    private static final String KEY_MUSIC_VOLUME = "musicVolume";

    public static final String DEFAULT_PLAYER_NAME = "Sobrevivente";
    public static final String DIFFICULTY_EASY = "Facil";
    public static final String DIFFICULTY_NORMAL = "Normal";
    public static final String DIFFICULTY_HARD = "Dificil";
    public static final String[] DIFFICULTIES = {
        DIFFICULTY_EASY,
        DIFFICULTY_NORMAL,
        DIFFICULTY_HARD
    };

    private static final float DEFAULT_MUSIC_VOLUME = 12f;

    public String playerName;
    public String difficulty;
    public boolean musicEnabled;
    public float musicVolume;

    public GameSettings() {
        playerName = DEFAULT_PLAYER_NAME;
        difficulty = DIFFICULTY_NORMAL;
        musicEnabled = true;
        musicVolume = DEFAULT_MUSIC_VOLUME;
    }

    public static GameSettings load() {
        Preferences preferences = Gdx.app.getPreferences(PREFS_NAME);
        GameSettings settings = new GameSettings();
        settings.playerName = cleanPlayerName(preferences.getString(KEY_PLAYER_NAME, DEFAULT_PLAYER_NAME));
        settings.difficulty = cleanDifficulty(preferences.getString(KEY_DIFFICULTY, DIFFICULTY_NORMAL));
        settings.musicEnabled = preferences.getBoolean(KEY_MUSIC_ENABLED, true);
        settings.musicVolume = clampVolume(preferences.getFloat(KEY_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME));
        return settings;
    }

    public void save() {
        playerName = cleanPlayerName(playerName);
        difficulty = cleanDifficulty(difficulty);
        musicVolume = clampVolume(musicVolume);

        Preferences preferences = Gdx.app.getPreferences(PREFS_NAME);
        preferences.putString(KEY_PLAYER_NAME, playerName);
        preferences.putString(KEY_DIFFICULTY, difficulty);
        preferences.putBoolean(KEY_MUSIC_ENABLED, musicEnabled);
        preferences.putFloat(KEY_MUSIC_VOLUME, musicVolume);
        preferences.flush();
    }

    public float getEnemySpawnMultiplier() {
        if (DIFFICULTY_EASY.equals(difficulty)) {
            return 0.75f;
        }
        if (DIFFICULTY_HARD.equals(difficulty)) {
            return 1.35f;
        }
        return 1f;
    }

    private static String cleanPlayerName(String value) {
        if (value == null) {
            return DEFAULT_PLAYER_NAME;
        }

        String cleanValue = value.trim();
        return cleanValue.isEmpty() ? DEFAULT_PLAYER_NAME : cleanValue;
    }

    private static String cleanDifficulty(String value) {
        for (String difficulty : DIFFICULTIES) {
            if (difficulty.equals(value)) {
                return difficulty;
            }
        }
        return DIFFICULTY_NORMAL;
    }

    private static float clampVolume(float value) {
        return Math.max(0f, Math.min(100f, value));
    }
}
