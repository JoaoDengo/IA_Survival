package io.github.AISurvivors.model.entity;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Player {
    private final Vector2 position = new Vector2();
    private final TextureRegion idleFrame;
    private final TextureRegion[] walkFrames;
    private final Sprite sprite;
    private final Sound stepSound;
    private int currentFrame;
    private float animationTimer;
    private float stepTimer;

    private static final float WORLD_SIZE = 1.8f;
    private static final float SPEED = 8f;
    private static final float FRAME_TIME = 0.12f;
    private static final float STEP_INTERVAL = 0.32f;
    private static final float STEP_VOLUME = 0.1f;


    public Player(Texture idleTexture, Texture walkTexture, Sound stepSound, Vector2 spawnPosition) {
        this.idleFrame = new TextureRegion(idleTexture);
        this.walkFrames = TextureRegion.split(walkTexture, 32, 32)[0];
        this.sprite = new Sprite(idleFrame);
        this.stepSound = stepSound;
        this.position.set(spawnPosition);
        this.animationTimer = FRAME_TIME;
        this.stepTimer = STEP_INTERVAL;
    }

    public void update(float delta, Vector2 movementDirection, float worldWidth, float worldHeight) {
        if (movementDirection.isZero()) {
            sprite.setRegion(idleFrame);
            stepTimer = STEP_INTERVAL;
            return;
        }
        float halfSize = WORLD_SIZE * 0.5f;
        position.x = MathUtils.clamp(position.x + (movementDirection.x * SPEED * delta), halfSize, worldWidth - halfSize);
        position.y = MathUtils.clamp(position.y + (movementDirection.y * SPEED * delta), halfSize, worldHeight - halfSize);

        animationTimer -= delta;
        if (animationTimer <= 0f) {
            currentFrame = (currentFrame + 1) % walkFrames.length;
            sprite.setRegion(walkFrames[currentFrame]);
            animationTimer += FRAME_TIME;
        }

        stepTimer -= delta;
        if (stepTimer <= 0f) {
            stepSound.play(STEP_VOLUME);
            stepTimer += STEP_INTERVAL;
        }
    }

    public void draw(SpriteBatch batch) {
        float drawX = position.x - (WORLD_SIZE * 0.5f);
        float drawY = position.y - (WORLD_SIZE * 0.5f);
        sprite.setBounds(drawX, drawY, WORLD_SIZE, WORLD_SIZE);
        sprite.draw(batch);
    }

    public Vector2 getPosition() {
        return position;
    }

    public Rectangle getBounds(Rectangle outBounds) {
        float size = WORLD_SIZE * 0.72f;
        return outBounds.set(position.x - (size * 0.5f), position.y - (size * 0.5f), size, size);
    }
}
