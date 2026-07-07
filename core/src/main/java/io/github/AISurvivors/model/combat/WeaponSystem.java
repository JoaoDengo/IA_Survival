package io.github.AISurvivors.model.combat;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import io.github.AISurvivors.model.entity.Bullet;
import io.github.AISurvivors.model.entity.Enemy;
import io.github.AISurvivors.model.spawn.EnemyManager;


public class WeaponSystem {
    private static final int MAX_ACTIVE_BULLETS = 64;
    private static final float FIRE_INTERVAL = 0.45f;   // segundos entre cada tiro
    private static final float TARGET_RANGE = 11f;      // alcance de mira (unidades do mundo)
    private static final float BASE_BULLET_DAMAGE = 1f;

    // aparencia da bala (tracer): rastro orientado na direcao do tiro
    private static final float TRAIL_LENGTH = 0.55f;    // comprimento do rastro
    private static final float TRAIL_WIDTH = 0.13f;     // espessura do corpo do rastro
    private static final float GLOW_WIDTH = 0.26f;      // espessura do brilho externo
    private static final float CORE_RADIUS = 0.11f;     // raio do nucleo brilhante na ponta

    // vetores reutilizados no desenho (evita criar lixo por frame)
    private final Vector2 tmpDirection = new Vector2();
    private final Vector2 tmpTail = new Vector2();
    private final Vector2 tmpHead = new Vector2();

    // array com as balas ativas (as que estao voando na tela)
    private final Array<Bullet> activeBullets = new Array<>(false, MAX_ACTIVE_BULLETS);

    // pool de balas: quando precisamos de uma nova, pegamos daqui (obtain);
    // quando ela "morre", devolvemos (free) ao inves de dar dispose.
    private final Pool<Bullet> bulletPool = new Pool<Bullet>(MAX_ACTIVE_BULLETS, MAX_ACTIVE_BULLETS) {
        @Override
        protected Bullet newObject() {
            return new Bullet();
        }
    };

    private float fireTimer;
    private float bulletDamage = BASE_BULLET_DAMAGE;

    public WeaponSystem() {
        // pre-preenche a pool para evitar alocacoes durante a partida
        bulletPool.fill(MAX_ACTIVE_BULLETS);
    }

    public void update(float delta, Vector2 playerPosition, EnemyManager enemyManager) {
        // 1) conta o tempo ate o proximo disparo
        fireTimer -= delta;
        if (fireTimer <= 0f) {
            if (tryFire(playerPosition, enemyManager)) {
                fireTimer += FIRE_INTERVAL;
            } else {
                // sem alvo no alcance: fica pronto para atirar assim que surgir um
                fireTimer = 0f;
            }
        }

        // 2) atualiza cada bala, checa acerto e devolve as "mortas" para a pool
        for (int i = activeBullets.size - 1; i >= 0; i--) {
            Bullet bullet = activeBullets.get(i);
            bullet.update(delta);

            if (bullet.isAlive() && enemyManager.damageFirstEnemyHit(bullet.getHitbox(), bullet.getDamage())) {
                bullet.markForRemoval();
            }

            if (!bullet.isAlive()) {
                activeBullets.removeIndex(i);
                bulletPool.free(bullet);
            }
        }
    }

    private boolean tryFire(Vector2 playerPosition, EnemyManager enemyManager) {
        if (activeBullets.size >= MAX_ACTIVE_BULLETS) {
            return false;
        }

        Enemy target = enemyManager.findNearestEnemy(playerPosition, TARGET_RANGE);
        if (target == null) {
            return false;
        }

        // direcao normalizada do player ate o alvo
        float dirX = target.getPosition().x - playerPosition.x;
        float dirY = target.getPosition().y - playerPosition.y;
        float length = (float) Math.sqrt((dirX * dirX) + (dirY * dirY));
        if (length < 0.0001f) {
            return false;
        }
        dirX /= length;
        dirY /= length;

        Bullet bullet = bulletPool.obtain();
        bullet.init(playerPosition.x, playerPosition.y, dirX, dirY, bulletDamage);
        activeBullets.add(bullet);
        return true;
    }

    /** Aumenta o dano das proximas balas (chamado quando o player sobe de nivel). */
    public void increaseDamage(float amount) {
        bulletDamage += amount;
    }

    public float getBulletDamage() {
        return bulletDamage;
    }

    /**
     * Desenha as balas como um "tracer": um rastro alongado na direcao do tiro,
     * com um brilho translucido em volta e um nucleo claro na ponta.
     * Deve ser chamado dentro de um passe {@code ShapeType.Filled} (com blend
     * ligado, por causa do brilho translucido).
     */
    public void draw(ShapeRenderer shapeRenderer) {
        for (int i = 0; i < activeBullets.size; i++) {
            Bullet bullet = activeBullets.get(i);
            Vector2 position = bullet.getPosition();

            // direcao normalizada do movimento (orienta o rastro)
            tmpDirection.set(bullet.getVelocity());
            if (tmpDirection.isZero()) {
                tmpDirection.set(1f, 0f);
            } else {
                tmpDirection.nor();
            }

            // ponta a frente da posicao, cauda atras dela
            tmpHead.set(position).mulAdd(tmpDirection, CORE_RADIUS * 0.5f);
            tmpTail.set(position).mulAdd(tmpDirection, -TRAIL_LENGTH);

            // 1) brilho externo (laranja translucido)
            shapeRenderer.setColor(1f, 0.55f, 0.1f, 0.35f);
            shapeRenderer.rectLine(tmpTail.x, tmpTail.y, tmpHead.x, tmpHead.y, GLOW_WIDTH);

            // 2) corpo do rastro (laranja/dourado solido)
            shapeRenderer.setColor(1f, 0.75f, 0.2f, 1f);
            shapeRenderer.rectLine(tmpTail.x, tmpTail.y, tmpHead.x, tmpHead.y, TRAIL_WIDTH);

            // 3) nucleo quente na ponta (quase branco)
            shapeRenderer.setColor(1f, 0.98f, 0.85f, 1f);
            shapeRenderer.circle(tmpHead.x, tmpHead.y, CORE_RADIUS, 10);
        }
    }

    /** Desenha as hitboxes das balas (usar dentro de um passe Line, modo debug). */
    public void drawDebug(ShapeRenderer shapeRenderer) {
        for (int i = 0; i < activeBullets.size; i++) {
            Bullet bullet = activeBullets.get(i);
            shapeRenderer.rect(
                bullet.getHitbox().x,
                bullet.getHitbox().y,
                bullet.getHitbox().width,
                bullet.getHitbox().height
            );
        }
    }

    public int getActiveCount() {
        return activeBullets.size;
    }

    public void dispose() {
        bulletPool.freeAll(activeBullets);
        activeBullets.clear();
        bulletPool.clear();
    }
}
