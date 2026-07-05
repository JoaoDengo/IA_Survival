package io.github.AISurvivors.model.collision;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Guarda os retangulos solidos do mapa (predios, escombros, monumento, muralha)
 * e resolve a colisao das entidades contra eles.
 *
 * <p>A colisao e feita "na mao" (sem Box2D), como sugerido no slide 08: cada
 * corpo e um retangulo (hitbox) e o solido tambem; a resolucao e por eixo
 * (move em X e corrige, depois move em Y e corrige), o que permite deslizar
 * ao longo das paredes de forma estavel em um jogo top-down.</p>
 */
public class CollisionWorld {
    private final Array<Rectangle> solids = new Array<>();

    public void addSolid(Rectangle rectangle) {
        solids.add(rectangle);
    }

    public Array<Rectangle> getSolids() {
        return solids;
    }

    /**
     * Move o corpo centrado em {@code position} (com meias-extensoes halfW/halfH)
     * pelo deslocamento (dx, dy), corrigindo a posicao para nao entrar nos solidos.
     */
    public void moveAndCollide(Vector2 position, float halfW, float halfH, float dx, float dy) {
        // eixo X
        position.x += dx;
        if (dx != 0f) {
            for (int i = 0; i < solids.size; i++) {
                Rectangle s = solids.get(i);
                if (overlaps(position, halfW, halfH, s)) {
                    position.x = dx > 0f ? s.x - halfW : s.x + s.width + halfW;
                }
            }
        }

        // eixo Y
        position.y += dy;
        if (dy != 0f) {
            for (int i = 0; i < solids.size; i++) {
                Rectangle s = solids.get(i);
                if (overlaps(position, halfW, halfH, s)) {
                    position.y = dy > 0f ? s.y - halfH : s.y + s.height + halfH;
                }
            }
        }
    }

    /** Verdadeiro se o ponto (com raio) esta dentro de algum solido. */
    public boolean overlapsPoint(float x, float y, float halfSize) {
        for (int i = 0; i < solids.size; i++) {
            Rectangle s = solids.get(i);
            if (x + halfSize > s.x && x - halfSize < s.x + s.width
                && y + halfSize > s.y && y - halfSize < s.y + s.height) {
                return true;
            }
        }
        return false;
    }

    private boolean overlaps(Vector2 position, float halfW, float halfH, Rectangle s) {
        return position.x + halfW > s.x
            && position.x - halfW < s.x + s.width
            && position.y + halfH > s.y
            && position.y - halfH < s.y + s.height;
    }

    public void drawDebug(ShapeRenderer shapeRenderer) {
        for (int i = 0; i < solids.size; i++) {
            Rectangle s = solids.get(i);
            shapeRenderer.rect(s.x, s.y, s.width, s.height);
        }
    }
}
