package io.github.AISurvivors.model.fx;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * Gerencia as particulas ativas e a pool de particulas reutilizaveis.
 *
 * <p>Mesmo padrao do {@link io.github.AISurvivors.model.spawn.EnemyManager} e do
 * {@link io.github.AISurvivors.model.combat.WeaponSystem}: um {@link Pool} para
 * reaproveitar objetos e um {@link Array} das particulas vivas, com o loop
 * reverso devolvendo as "mortas" para a pool.</p>
 */
public class ParticleSystem {
    private static final int MAX_PARTICLES = 400;
    private static final int SPARKS_PER_BURST = 12;
    private static final int SMOKE_PER_BURST = 5;

    private final Array<Particle> active = new Array<>(false, MAX_PARTICLES);
    private final Pool<Particle> pool = new Pool<Particle>(MAX_PARTICLES, MAX_PARTICLES) {
        @Override
        protected Particle newObject() {
            return new Particle();
        }
    };

    public ParticleSystem() {
        pool.fill(MAX_PARTICLES);
    }

    /** Dispara uma explosao de faiscas + fumaca na posicao (x, y). */
    public void burst(float x, float y) {
        // faiscas: rapidas, pequenas, quentes (amarelo/laranja), vida curta
        for (int i = 0; i < SPARKS_PER_BURST; i++) {
            if (active.size >= MAX_PARTICLES) {
                return;
            }
            float angle = MathUtils.random(MathUtils.PI2);
            float speed = MathUtils.random(3.5f, 8f);
            float life = MathUtils.random(0.25f, 0.5f);
            float size = MathUtils.random(0.09f, 0.17f);
            float g = MathUtils.random(0.6f, 0.95f);
            spawn(x, y, MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed,
                life, size, 1f, g, 0.2f);
        }

        // fumaca: lenta, maior, cinza, vida um pouco maior
        for (int i = 0; i < SMOKE_PER_BURST; i++) {
            if (active.size >= MAX_PARTICLES) {
                return;
            }
            float angle = MathUtils.random(MathUtils.PI2);
            float speed = MathUtils.random(0.6f, 2f);
            float life = MathUtils.random(0.5f, 0.9f);
            float size = MathUtils.random(0.25f, 0.42f);
            float shade = MathUtils.random(0.25f, 0.45f);
            spawn(x, y, MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed,
                life, size, shade, shade, shade);
        }
    }

    private void spawn(float x, float y, float vx, float vy, float life, float size,
                       float r, float g, float b) {
        Particle p = pool.obtain();
        p.init(x, y, vx, vy, life, size, r, g, b);
        active.add(p);
    }

    public void update(float delta) {
        for (int i = active.size - 1; i >= 0; i--) {
            Particle p = active.get(i);
            p.update(delta);
            if (!p.isAlive()) {
                active.removeIndex(i);
                pool.free(p);
            }
        }
    }

    /** Desenha as particulas (usar dentro de um passe {@code Filled} com blend ligado). */
    public void draw(ShapeRenderer shapeRenderer) {
        for (int i = 0; i < active.size; i++) {
            Particle p = active.get(i);
            shapeRenderer.setColor(p.getRed(), p.getGreen(), p.getBlue(), p.getAlpha());
            shapeRenderer.circle(p.getX(), p.getY(), p.getRadius(), 8);
        }
    }

    public int getActiveCount() {
        return active.size;
    }

    public void dispose() {
        pool.freeAll(active);
        active.clear();
        pool.clear();
    }
}
