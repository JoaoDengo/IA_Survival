package io.github.AISurvivors.model.spawn;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import io.github.AISurvivors.model.entity.Enemy;

public class EnemyManager {
    private static final int MAX_ACTIVE_ENEMIES = 65;
    private static final int MAX_SPAWNS_PER_FRAME = 8;
    private static final int FINAL_HORDE_SIZE = 8;
    private static final float FINAL_HORDE_TIME = 55f;
    private static final float SPAWN_MARGIN = 1.5f;
    private static final float ENEMY_HALF_SIZE = 0.9f;
    private static final float MIN_ENEMY_DISTANCE = 1.05f;
    private static final int SPAWN_ATTEMPTS = 16;

    private final Array<Enemy> activeEnemies = new Array<>(false, MAX_ACTIVE_ENEMIES);
    private final Pool<Enemy> enemyPool;
    private final float worldWidth;
    private final float worldHeight;
    private float elapsedTime;
    private float spawnBudget;
    private boolean finalHordeSpawned;

    public EnemyManager(Texture enemyIdleTexture, Texture enemyWalkTexture, float worldWidth, float worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        enemyPool = new Pool<>(MAX_ACTIVE_ENEMIES, MAX_ACTIVE_ENEMIES) {
            @Override
            protected Enemy newObject() {
                return new Enemy(enemyIdleTexture, enemyWalkTexture);
            }
        };

        enemyPool.fill(MAX_ACTIVE_ENEMIES);
    }

    public void update(float delta, Vector2 playerPosition, float visibleWidth, float visibleHeight) {
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
            enemy.update(delta, playerPosition, worldWidth, worldHeight);

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

            Enemy enemy = enemyPool.obtain();
            enemy.init(spawnX, spawnY);
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
        if (elapsedTime < 10f) {
            return 0.8f;
        }
        if (elapsedTime < 25f) {
            return 1.3f;
        }
        if (elapsedTime < 40f) {
            return 2f;
        }
        if (elapsedTime < 55f) {
            return 3f;
        }
        return 3.5f;
    }

    public int getActiveLimit() {
        if (elapsedTime < 10f) {
            return 15;
        }
        if (elapsedTime < 25f) {
            return 30;
        }
        if (elapsedTime < 40f) {
            return 45;
        }
        return MAX_ACTIVE_ENEMIES;
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
