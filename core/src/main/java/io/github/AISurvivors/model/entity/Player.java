package io.github.AISurvivors.model.entity;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.github.AISurvivors.model.collision.CollisionWorld;

public class Player {
    private final Vector2 position = new Vector2();
    private final TextureRegion idleFrame;
    private final TextureRegion[] walkFrames;
    private final Sprite sprite;
    private final Sound stepSound;
    private int currentFrame;
    private float animationTimer;
    private float stepTimer;
    private float health;
    private float invincibleTimer;

    private static final float WORLD_SIZE = 1.8f;
    private static final float SPEED = 8f;
    private static final float FRAME_TIME = 0.12f;
    private static final float STEP_INTERVAL = 0.32f;
    private static final float STEP_VOLUME = 0.1f;
    private static final float MAX_HEALTH = 100f;
    private static final float INVINCIBLE_TIME = 0.8f;  // segundos de invencibilidade apos levar dano
    private static final float BLINK_INTERVAL = 0.08f;  // ritmo da piscada durante a invencibilidade
    private static final float COLLISION_HALF = WORLD_SIZE * 0.3f;  // meia-caixa de colisao com o mapa


    public Player(Texture idleTexture, Texture walkTexture, Sound stepSound, Vector2 spawnPosition) {
        this.idleFrame = new TextureRegion(idleTexture);
        this.walkFrames = TextureRegion.split(walkTexture, 32, 32)[0];
        this.sprite = new Sprite(idleFrame);
        this.stepSound = stepSound;
        this.position.set(spawnPosition);
        this.animationTimer = FRAME_TIME;
        this.stepTimer = STEP_INTERVAL;
        this.health = MAX_HEALTH;
    }

    public void update(float delta, Vector2 movementDirection, float worldWidth, float worldHeight, CollisionWorld collisionWorld) {
        // a invencibilidade corre sempre, mesmo parado
        if (invincibleTimer > 0f) {
            invincibleTimer -= delta;
        }
        if (movementDirection.isZero()) {
            sprite.setRegion(idleFrame);
            stepTimer = STEP_INTERVAL;
            return;
        }

        float dx = movementDirection.x * SPEED * delta;
        float dy = movementDirection.y * SPEED * delta;

        // colisao contra os solidos do mapa (predios, escombros, muralha)
        if (collisionWorld != null) {
            collisionWorld.moveAndCollide(position, COLLISION_HALF, COLLISION_HALF, dx, dy);
        } else {
            position.add(dx, dy);
        }

        // limites da arena
        float halfSize = WORLD_SIZE * 0.5f;
        position.x = MathUtils.clamp(position.x, halfSize, worldWidth - halfSize);
        position.y = MathUtils.clamp(position.y, halfSize, worldHeight - halfSize);

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

        // durante a invencibilidade o player pisca (fica semi-transparente)
        boolean blinkOff = invincibleTimer > 0f
            && ((int) (invincibleTimer / BLINK_INTERVAL) & 1) == 0;
        sprite.setAlpha(blinkOff ? 0.35f : 1f);
        sprite.draw(batch);
        sprite.setAlpha(1f);
    }

    /**
     * Aplica dano ao player. Ignora o dano se ele ainda estiver invencivel
     * (i-frames) do golpe anterior. Retorna {@code true} se o dano foi aplicado.
     */
    public boolean takeDamage(float amount) {
        if (invincibleTimer > 0f || amount <= 0f || health <= 0f) {
            return false;
        }

        health = Math.max(0f, health - amount);
        invincibleTimer = INVINCIBLE_TIME;
        return true;
    }

    public boolean isAlive() {
        return health > 0f;
    }

    public float getHealth() {
        return health;
    }

    public float getMaxHealth() {
        return MAX_HEALTH;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Rectangle getBounds(Rectangle outBounds) {
        float size = WORLD_SIZE * 0.72f;
        return outBounds.set(position.x - (size * 0.5f), position.y - (size * 0.5f), size, size);
    }
}
