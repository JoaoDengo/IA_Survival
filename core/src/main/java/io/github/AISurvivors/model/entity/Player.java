package io.github.AISurvivors.model.entity;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Player {
    private static final float WORLD_SIZE = 1.8f;
    private static final float SPEED = 8f;
    private static final float STEP_INTERVAL = 0.32f;
    private static final float STEP_VOLUME = 0.1f;

    private final Vector2 position = new Vector2();
    private final TextureRegion idleFrame;
    private final Animation<TextureRegion> walkAnimation;
    private final Sound stepSound;
    private float animationTime;
    private float stepTimer;

    public Player(Texture idleTexture, Texture walkTexture, Sound stepSound, Vector2 spawnPosition) {
        this.idleFrame = new TextureRegion(idleTexture);
        this.walkAnimation = new Animation<>(0.12f, TextureRegion.split(walkTexture, 32, 32)[0]);
        this.walkAnimation.setPlayMode(Animation.PlayMode.LOOP);
        this.stepSound = stepSound;
        this.position.set(spawnPosition);
    }

    public void update(float delta, Vector2 movementDirection, float worldWidth, float worldHeight) {
        if (movementDirection.isZero()) {
            stepTimer = 0f;
            return;
        }

        float halfSize = WORLD_SIZE * 0.5f;
        position.x = MathUtils.clamp(position.x + (movementDirection.x * SPEED * delta), halfSize, worldWidth - halfSize);
        position.y = MathUtils.clamp(position.y + (movementDirection.y * SPEED * delta), halfSize, worldHeight - halfSize);

        animationTime += delta;
        stepTimer -= delta;

        if (stepTimer <= 0f) {
            stepSound.play(STEP_VOLUME);
            stepTimer = STEP_INTERVAL;
        }
    }

    public void draw(SpriteBatch batch, boolean isMoving) {
        TextureRegion currentFrame = isMoving ? walkAnimation.getKeyFrame(animationTime, true) : idleFrame;
        float drawX = position.x - (WORLD_SIZE * 0.5f);
        float drawY = position.y - (WORLD_SIZE * 0.5f);
        batch.draw(currentFrame, drawX, drawY, WORLD_SIZE, WORLD_SIZE);
    }

    public Vector2 getPosition() {
        return position;
    }
}
