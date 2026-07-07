package io.github.AISurvivors.model.fx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;


public class ParticleSystem implements Disposable {
    private static final int MAX_ACTIVE_BURSTS = 32;

    private final TextureAtlas atlas;
    private final ParticleEffectPool burstPool;
    private final Array<PooledEffect> activeBursts = new Array<>(false, MAX_ACTIVE_BURSTS);

    public ParticleSystem() {
        atlas = new TextureAtlas(Gdx.files.internal("particles/spark.atlas"));

        ParticleEffect sparkEffect = new ParticleEffect();
        sparkEffect.load(Gdx.files.internal("particles/spark.p"), atlas);

        burstPool = new ParticleEffectPool(sparkEffect, 0, MAX_ACTIVE_BURSTS);
    }

    /** Dispara uma explosao de particulas na posicao (x, y), ex: morte de um inimigo. */
    public void burst(float x, float y) {
        if (activeBursts.size >= MAX_ACTIVE_BURSTS) {
            return;
        }

        PooledEffect effect = burstPool.obtain();
        effect.setPosition(x, y);
        effect.start();
        activeBursts.add(effect);
    }

    public void update(float delta) {
        for (int i = activeBursts.size - 1; i >= 0; i--) {
            PooledEffect effect = activeBursts.get(i);
            effect.update(delta);
            if (effect.isComplete()) {
                activeBursts.removeIndex(i);
                effect.free();
            }
        }
    }

    /** Desenha as explosoes ativas (dentro de um passe de {@link Batch} ja aberto). */
    public void draw(Batch batch) {
        for (int i = 0; i < activeBursts.size; i++) {
            activeBursts.get(i).draw(batch);
        }
    }

    public int getActiveCount() {
        return activeBursts.size;
    }

    @Override
    public void dispose() {
        activeBursts.clear();
        atlas.dispose();
    }
}
