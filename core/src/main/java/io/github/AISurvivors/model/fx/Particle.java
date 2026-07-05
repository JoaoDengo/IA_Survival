package io.github.AISurvivors.model.fx;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;

/**
 * Uma unica particula (faisca ou fumaca) do efeito de morte do inimigo.
 *
 * <p>Como no slide de particulas 2D, cada particula e uma "entidade
 * independente": tem posicao, velocidade e tempo de vida proprios. E tambem
 * segue o object pooling (slide 04) — implementa {@link Pool.Poolable} com
 * {@link #init} e {@link #reset()} — para reaproveitarmos particulas em vez de
 * criar objetos novos a cada morte.</p>
 */
public class Particle implements Pool.Poolable {
    private static final float FRICTION = 3f;  // desaceleracao (dá o efeito de "estourar e frear")

    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private float life;
    private float maxLife;
    private float baseSize;
    private float red, green, blue;
    private boolean alive;

    public void init(float x, float y, float vx, float vy, float life, float size,
                     float red, float green, float blue) {
        position.set(x, y);
        velocity.set(vx, vy);
        this.life = life;
        this.maxLife = life;
        this.baseSize = size;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alive = true;
    }

    public void update(float delta) {
        if (!alive) {
            return;
        }

        position.mulAdd(velocity, delta);
        velocity.scl(Math.max(0f, 1f - FRICTION * delta));  // frenagem gradual
        life -= delta;
        if (life <= 0f) {
            alive = false;
        }
    }

    public boolean isAlive() {
        return alive;
    }

    public float getX() {
        return position.x;
    }

    public float getY() {
        return position.y;
    }

    /** Alpha (0..1) que decai com o tempo de vida, para a particula "apagar". */
    public float getAlpha() {
        return maxLife <= 0f ? 0f : Math.max(0f, life / maxLife);
    }

    /** Raio atual: encolhe conforme a particula morre. */
    public float getRadius() {
        return baseSize * (0.35f + 0.65f * getAlpha());
    }

    public float getRed() {
        return red;
    }

    public float getGreen() {
        return green;
    }

    public float getBlue() {
        return blue;
    }

    @Override
    public void reset() {
        position.setZero();
        velocity.setZero();
        life = 0f;
        maxLife = 0f;
        baseSize = 0f;
        red = green = blue = 0f;
        alive = false;
    }
}
