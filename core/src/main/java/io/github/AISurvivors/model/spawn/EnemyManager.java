package io.github.AISurvivors.model.spawn;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import io.github.AISurvivors.model.collision.CollisionWorld;
import io.github.AISurvivors.model.entity.Enemy;

public class EnemyManager {
    /** Notificado quando um inimigo morre (para efeitos como particulas). */
    public interface DeathListener {
        void onEnemyDeath(float x, float y);
    }

    private static final int MAX_ACTIVE_ENEMIES = 65;
    private static final int MAX_SPAWNS_PER_FRAME = 8;
    private static final int FINAL_HORDE_SIZE = 8;
    private static final float FINAL_HORDE_TIME = 55f;
    private static final float SPAWN_MARGIN = 1.5f;
    private static final float ENEMY_HALF_SIZE = 0.9f;
    private static final float MIN_ENEMY_DISTANCE = 1.05f;
    private static final int SPAWN_ATTEMPTS = 16;
    private static final float BASE_ENEMY_HEALTH = 3f;
    private static final float ENEMY_HEALTH_PER_LEVEL = 1f;

    private final Array<Enemy> activeEnemies = new Array<>(false, MAX_ACTIVE_ENEMIES);
    private final Pool<Enemy> enemyPool;
    private final float worldWidth;
    private final float worldHeight;
    private final float spawnMultiplier;
    private float elapsedTime;
    private float spawnBudget;
    private boolean finalHordeSpawned;
    private int killCount;
    private float spawnHealth = BASE_ENEMY_HEALTH;
    private CollisionWorld collisionWorld;
    private DeathListener deathListener;

    public void setDeathListener(DeathListener deathListener) {
        this.deathListener = deathListener;
    }

    public EnemyManager(
        Texture enemyIdleTexture,
        Texture enemyWalkTexture,
        float worldWidth,
        float worldHeight,
        float spawnMultiplier
    ) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.spawnMultiplier = MathUtils.clamp(spawnMultiplier, 0.5f, 1.5f);
        enemyPool = new Pool<>(MAX_ACTIVE_ENEMIES, MAX_ACTIVE_ENEMIES) {
            @Override
            protected Enemy newObject() {
                return new Enemy(enemyIdleTexture, enemyWalkTexture);
            }
        };

        enemyPool.fill(MAX_ACTIVE_ENEMIES);
    }

    public void update(float delta, Vector2 playerPosition, float visibleWidth, float visibleHeight,
                       CollisionWorld collisionWorld) {
        this.collisionWorld = collisionWorld;
        elapsedTime += delta;
        spawnBudget = Math.min(
            spawnBudget + (getSpawnRate() * delta),
            MAX_SPAWNS_PER_FRAME
        );

        int activeLimit = getActiveLimit();
        int spawnsThisFrame = 0;
        while (spawnBudget >= 1f
            && activeEnemies.size < activeLimit
            && spawnsThisFrame < MAX_SPAWNS_PER_FRAME) {
            if (!spawnAroundPlayer(playerPosition, visibleWidth, visibleHeight)) {
                break;
            }

            spawnBudget -= 1f;
            spawnsThisFrame++;
        }

        if (!finalHordeSpawned && elapsedTime >= FINAL_HORDE_TIME) {
            finalHordeSpawned = true;
            spawnHorde(playerPosition, visibleWidth, visibleHeight);
        }

        float despawnDistance = (Math.max(visibleWidth, visibleHeight) * 1.75f) + SPAWN_MARGIN;
        float despawnDistanceSquared = despawnDistance * despawnDistance;

        for (int i = activeEnemies.size - 1; i >= 0; i--) {
            Enemy enemy = activeEnemies.get(i);
            enemy.update(delta, playerPosition, worldWidth, worldHeight, collisionWorld);

            if (enemy.getPosition().dst2(playerPosition) > despawnDistanceSquared) {
                enemy.markForRemoval();
            }

            if (!enemy.isAlive()) {
                activeEnemies.removeIndex(i);
                enemyPool.free(enemy);
            }
        }

        resolveEnemyOverlaps();
    }

    private void spawnHorde(Vector2 playerPosition, float visibleWidth, float visibleHeight) {
        for (int i = 0; i < FINAL_HORDE_SIZE && activeEnemies.size < MAX_ACTIVE_ENEMIES; i++) {
            if (!spawnAroundPlayer(playerPosition, visibleWidth, visibleHeight)) {
                break;
            }
        }
    }

    private boolean spawnAroundPlayer(Vector2 playerPosition, float visibleWidth, float visibleHeight) {
        float halfVisibleWidth = visibleWidth * 0.5f;
        float halfVisibleHeight = visibleHeight * 0.5f;

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            float spawnX;
            float spawnY;

            switch (MathUtils.random(3)) {
                case 0:
                    spawnX = playerPosition.x - halfVisibleWidth - SPAWN_MARGIN;
                    spawnY = playerPosition.y + MathUtils.random(-halfVisibleHeight, halfVisibleHeight);
                    break;
                case 1:
                    spawnX = playerPosition.x + halfVisibleWidth + SPAWN_MARGIN;
                    spawnY = playerPosition.y + MathUtils.random(-halfVisibleHeight, halfVisibleHeight);
                    break;
                case 2:
                    spawnX = playerPosition.x + MathUtils.random(-halfVisibleWidth, halfVisibleWidth);
                    spawnY = playerPosition.y - halfVisibleHeight - SPAWN_MARGIN;
                    break;
                default:
                    spawnX = playerPosition.x + MathUtils.random(-halfVisibleWidth, halfVisibleWidth);
                    spawnY = playerPosition.y + halfVisibleHeight + SPAWN_MARGIN;
                    break;
            }

            spawnX = MathUtils.clamp(spawnX, ENEMY_HALF_SIZE, worldWidth - ENEMY_HALF_SIZE);
            spawnY = MathUtils.clamp(spawnY, ENEMY_HALF_SIZE, worldHeight - ENEMY_HALF_SIZE);

            boolean outsideVisibleArea =
                Math.abs(spawnX - playerPosition.x) > halfVisibleWidth
                    || Math.abs(spawnY - playerPosition.y) > halfVisibleHeight;
            if (!outsideVisibleArea) {
                continue;
            }

            // nao nasce dentro de um predio/escombro solido
            if (collisionWorld != null && collisionWorld.overlapsPoint(spawnX, spawnY, ENEMY_HALF_SIZE)) {
                continue;
            }

            Enemy enemy = enemyPool.obtain();
            enemy.init(spawnX, spawnY, spawnHealth);
            activeEnemies.add(enemy);
            return true;
        }

        return false;
    }

    private void resolveEnemyOverlaps() {
        float minimumDistanceSquared = MIN_ENEMY_DISTANCE * MIN_ENEMY_DISTANCE;

        for (int i = 0; i < activeEnemies.size; i++) {
            Enemy first = activeEnemies.get(i);

            for (int j = i + 1; j < activeEnemies.size; j++) {
                Enemy second = activeEnemies.get(j);
                float distanceX = first.getPosition().x - second.getPosition().x;
                float distanceY = first.getPosition().y - second.getPosition().y;
                float distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);

                if (distanceSquared >= minimumDistanceSquared) {
                    continue;
                }

                if (distanceSquared < 0.0001f) {
                    distanceX = ((i + j) & 1) == 0 ? 0.01f : -0.01f;
                    distanceY = 0.01f;
                    distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);
                }

                float distance = (float) Math.sqrt(distanceSquared);
                float pushDistance = (MIN_ENEMY_DISTANCE - distance) * 0.5f;
                float normalX = distanceX / distance;
                float normalY = distanceY / distance;

                first.moveBy(normalX * pushDistance, normalY * pushDistance, worldWidth, worldHeight);
                second.moveBy(-normalX * pushDistance, -normalY * pushDistance, worldWidth, worldHeight);
            }
        }
    }

    private float getSpawnRate() {
        float baseRate;
        if (elapsedTime < 10f) {
            baseRate = 0.8f;
        } else if (elapsedTime < 25f) {
            baseRate = 1.3f;
        } else if (elapsedTime < 40f) {
            baseRate = 2f;
        } else if (elapsedTime < 55f) {
            baseRate = 3f;
        } else {
            baseRate = 3.5f;
        }

        return baseRate * spawnMultiplier;
    }

    public int getActiveLimit() {
        int baseLimit;
        if (elapsedTime < 10f) {
            baseLimit = 15;
        } else if (elapsedTime < 25f) {
            baseLimit = 30;
        } else if (elapsedTime < 40f) {
            baseLimit = 45;
        } else {
            baseLimit = MAX_ACTIVE_ENEMIES;
        }

        return MathUtils.clamp(Math.round(baseLimit * spawnMultiplier), 1, MAX_ACTIVE_ENEMIES);
    }

    /**
     * Retorna o inimigo vivo mais proximo de {@code from} dentro de
     * {@code maxRange}, ou {@code null} se nao houver nenhum no alcance.
     * Usado pela arma do player para escolher o alvo.
     */
    public Enemy findNearestEnemy(Vector2 from, float maxRange) {
        Enemy nearest = null;
        float bestDistanceSquared = maxRange * maxRange;

        for (int i = 0; i < activeEnemies.size; i++) {
            Enemy enemy = activeEnemies.get(i);
            if (!enemy.isAlive()) {
                continue;
            }

            float distanceSquared = enemy.getPosition().dst2(from);
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                nearest = enemy;
            }
        }

        return nearest;
    }

    /**
     * Aplica dano ao primeiro inimigo cuja hitbox colide com {@code bulletHitbox}
     * (slide 08). Retorna {@code true} se acertou alguem, para que a bala saiba
     * que deve ser destruida.
     */
    public boolean damageFirstEnemyHit(Rectangle bulletHitbox, float damage) {
        for (int i = 0; i < activeEnemies.size; i++) {
            Enemy enemy = activeEnemies.get(i);
            if (enemy.overlaps(bulletHitbox)) {
                enemy.takeDamage(damage);
                if (!enemy.isAlive()) {
                    killCount++;
                    if (deathListener != null) {
                        deathListener.onEnemyDeath(enemy.getPosition().x, enemy.getPosition().y);
                    }
                }
                return true;
            }
        }

        return false;
    }

    public int getKillCount() {
        return killCount;
    }

    /**
     * Ajusta a vida com que os proximos inimigos vao nascer, de acordo com o
     * nivel do player. Os inimigos ja vivos mantem a vida que tinham.
     */
    public void scaleToPlayerLevel(int playerLevel) {
        spawnHealth = BASE_ENEMY_HEALTH + (playerLevel - 1) * ENEMY_HEALTH_PER_LEVEL;
    }

    public float getSpawnHealth() {
        return spawnHealth;
    }

    /**
     * Retorna {@code true} se a hitbox de algum inimigo vivo esta encostando na
     * hitbox do player (slide 08). Usado para causar dano por contato.
     */
    public boolean isTouchingPlayer(Rectangle playerHitbox) {
        for (int i = 0; i < activeEnemies.size; i++) {
            if (activeEnemies.get(i).overlaps(playerHitbox)) {
                return true;
            }
        }

        return false;
    }

    public void draw(SpriteBatch batch) {
        for (int i = 0; i < activeEnemies.size; i++) {
            activeEnemies.get(i).draw(batch);
        }
    }

    public void drawDebug(ShapeRenderer shapeRenderer) {
        for (int i = 0; i < activeEnemies.size; i++) {
            Enemy enemy = activeEnemies.get(i);
            shapeRenderer.rect(
                enemy.getHitbox().x,
                enemy.getHitbox().y,
                enemy.getHitbox().width,
                enemy.getHitbox().height
            );
        }
    }

    public int getActiveCount() {
        return activeEnemies.size;
    }

    public int getFreeCount() {
        return enemyPool.getFree();
    }

    public float getElapsedTime() {
        return elapsedTime;
    }

    public void dispose() {
        enemyPool.freeAll(activeEnemies);
        activeEnemies.clear();
        enemyPool.clear();
    }
}
