package io.github.AISurvivors.model.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;

public class Enemy implements Pool.Poolable {
    private static final float WORLD_SIZE = 1.8f;
    private static final float HITBOX_WIDTH = WORLD_SIZE * (20f / 32f);
    private static final float HITBOX_HEIGHT = WORLD_SIZE * (30f / 32f);
    private static final float SPEED = 2.6f;
    private static final float FRAME_TIME = 0.12f;
    private static final float STOP_DISTANCE = 0.8f;
    private static final float MAX_HEALTH = 3f;

    private final Vector2 position = new Vector2();
    private final Rectangle hitbox = new Rectangle();
    private final TextureRegion idleFrame;
    private final TextureRegion[] walkFrames;
    private final Sprite sprite;
    private int currentFrame;
    private float animationTimer;
    private float health;
    private boolean alive;

    public Enemy(Texture idleTexture, Texture walkTexture) {
        idleFrame = new TextureRegion(idleTexture);
        walkFrames = TextureRegion.split(walkTexture, 32, 32)[0];
        sprite = new Sprite(idleFrame);
        reset();
    }

    public void init(float x, float y) {
        position.set(x, y);
        health = MAX_HEALTH;
        alive = true;
        currentFrame = 0;
        animationTimer = FRAME_TIME;
        sprite.setRegion(idleFrame);
        updateHitbox();
    }

    public void update(float delta, Vector2 target, float worldWidth, float worldHeight) {
        if (!alive) {
            return;
        }

        float directionX = target.x - position.x;
        float directionY = target.y - position.y;
        float distanceSquared = (directionX * directionX) + (directionY * directionY);
        if (distanceSquared > STOP_DISTANCE * STOP_DISTANCE) {
            float inverseDistance = 1f / (float) Math.sqrt(distanceSquared);
            position.add(
                directionX * inverseDistance * SPEED * delta,
                directionY * inverseDistance * SPEED * delta
            );

            animationTimer -= delta;
            if (animationTimer <= 0f) {
                currentFrame = (currentFrame + 1) % walkFrames.length;
                sprite.setRegion(walkFrames[currentFrame]);
                animationTimer += FRAME_TIME;
            }
        } else {
            sprite.setRegion(idleFrame);
            animationTimer = FRAME_TIME;
        }

        clampToWorld(worldWidth, worldHeight);
        updateHitbox();
    }

    public void draw(SpriteBatch batch) {
        float drawX = position.x - (WORLD_SIZE * 0.5f);
        float drawY = position.y - (WORLD_SIZE * 0.5f);
        sprite.setBounds(drawX, drawY, WORLD_SIZE, WORLD_SIZE);
        sprite.draw(batch);
    }

    public void takeDamage(float damage) {
        if (!alive || damage <= 0f) {
            return;
        }

        health -= damage;
        if (health <= 0f) {
            alive = false;
        }
    }

    public void markForRemoval() {
        alive = false;
    }

    public void moveBy(float x, float y, float worldWidth, float worldHeight) {
        position.add(x, y);
        clampToWorld(worldWidth, worldHeight);
        updateHitbox();
    }

    private void clampToWorld(float worldWidth, float worldHeight) {
        float halfSize = WORLD_SIZE * 0.5f;
        position.x = MathUtils.clamp(position.x, halfSize, worldWidth - halfSize);
        position.y = MathUtils.clamp(position.y, halfSize, worldHeight - halfSize);
    }

    public Vector2 getPosition() {
        return position;
    }

    private void updateHitbox() {
        hitbox.set(
            position.x - (HITBOX_WIDTH * 0.5f),
            position.y - (HITBOX_HEIGHT * 0.5f),
            HITBOX_WIDTH,
            HITBOX_HEIGHT
        );
    }

    public Rectangle getHitbox() {
        return hitbox;
    }

    public boolean overlaps(Rectangle otherHitbox) {
        return alive && hitbox.overlaps(otherHitbox);
    }

    public boolean isAlive() {
        return alive;
    }

    @Override
    public void reset() {
        position.setZero();
        hitbox.set(0f, 0f, 0f, 0f);
        health = 0f;
        alive = false;
        currentFrame = 0;
        animationTimer = FRAME_TIME;
        sprite.setRegion(idleFrame);
        sprite.setFlip(false, false);
        sprite.setColor(1f, 1f, 1f, 1f);
    }
}
