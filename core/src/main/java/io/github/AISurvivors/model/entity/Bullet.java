package io.github.AISurvivors.model.entity;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;

/**
 * Projetil disparado pela arma automatica do player.
 *
 * <p>Segue o padrao de <b>object pooling</b> visto em aula (slide 04): a classe
 * implementa {@link Pool.Poolable}, possui {@link #init} para "nascer" e
 * {@link #reset()}, que e chamado automaticamente quando a bala volta para a
 * pool (assim nao criamos um objeto novo a cada tiro).</p>
 *
 * <p>Alem disso, a bala tem uma <b>hitbox</b> retangular (slide 08). Quando essa
 * hitbox encosta na hitbox de um inimigo, contabilizamos um acerto.</p>
 */
public class Bullet implements Pool.Poolable {
    private static final float RADIUS = 0.18f;          // raio visual da bala (em unidades do mundo)
    private static final float HITBOX_SIZE = RADIUS * 2f;
    private static final float SPEED = 12f;             // unidades por segundo
    private static final float MAX_LIFETIME = 1.4f;     // segundos ate a bala sumir sozinha

    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Rectangle hitbox = new Rectangle();
    private float lifetime;
    private float damage;
    private boolean alive;

    /**
     * Inicializa a bala depois de obte-la da pool.
     * A direcao (dirX, dirY) deve estar normalizada.
     */
    public void init(float x, float y, float dirX, float dirY, float damage) {
        position.set(x, y);
        velocity.set(dirX, dirY).scl(SPEED);
        this.damage = damage;
        lifetime = MAX_LIFETIME;
        alive = true;
        updateHitbox();
    }

    /** Atualiza a posicao da bala. Chamado em todo frame (no render). */
    public void update(float delta) {
        if (!alive) {
            return;
        }

        position.mulAdd(velocity, delta);
        lifetime -= delta;
        if (lifetime <= 0f) {
            alive = false;
        }

        updateHitbox();
    }

    private void updateHitbox() {
        hitbox.set(position.x - RADIUS, position.y - RADIUS, HITBOX_SIZE, HITBOX_SIZE);
    }

    /** Marca a bala como "morta" para que volte para a pool no proximo update. */
    public void markForRemoval() {
        alive = false;
    }

    public boolean isAlive() {
        return alive;
    }

    public float getDamage() {
        return damage;
    }

    public float getRadius() {
        return RADIUS;
    }

    public Vector2 getPosition() {
        return position;
    }

    /** Velocidade atual (usada para orientar o rastro/tracer na hora de desenhar). */
    public Vector2 getVelocity() {
        return velocity;
    }

    public Rectangle getHitbox() {
        return hitbox;
    }

    /**
     * Metodo chamado automaticamente quando a bala e liberada de volta para a
     * pool. Deixa o objeto "zerado" para o proximo uso.
     */
    @Override
    public void reset() {
        position.setZero();
        velocity.setZero();
        hitbox.set(0f, 0f, 0f, 0f);
        lifetime = 0f;
        damage = 0f;
        alive = false;
    }
}
