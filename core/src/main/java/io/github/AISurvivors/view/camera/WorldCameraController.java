package io.github.AISurvivors.view.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class WorldCameraController {
    private static final float MIN_ZOOM = 0.65f;
    private static final float MAX_ZOOM = 2.2f;
    private static final float ZOOM_STEP = 0.12f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final Vector2 focusTarget = new Vector2();
    private final Vector2 focusOffset = new Vector2();
    private final Vector2 tmpWorldBefore = new Vector2();
    private final Vector2 tmpWorldAfter = new Vector2();
    private final Vector2 tmpProjected = new Vector2();
    private final Vector3 tmpVector = new Vector3();
    private final float worldWidth;
    private final float worldHeight;

    public WorldCameraController(float viewportWidth, float viewportHeight, float worldWidth, float worldHeight) {
        this.viewport = new FitViewport(viewportWidth, viewportHeight, camera);
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        camera.zoom = 1f;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
        update();
    }


    public void resetOffset() {
        focusOffset.setZero();
    }

    public void panByScreenDrag(float previousScreenX, float previousScreenY, float currentScreenX, float currentScreenY) {
        screenToWorld(previousScreenX, previousScreenY, tmpWorldBefore);
        screenToWorld(currentScreenX, currentScreenY, tmpWorldAfter);
        focusOffset.add(tmpWorldBefore.x - tmpWorldAfter.x, tmpWorldBefore.y - tmpWorldAfter.y);
        update();
    }

    public void zoomAt(float screenX, float screenY, float zoomAmount) {
        screenToWorld(screenX, screenY, tmpWorldBefore);

        float nextZoom = MathUtils.clamp(camera.zoom + (zoomAmount * ZOOM_STEP), MIN_ZOOM, MAX_ZOOM);
        if (MathUtils.isEqual(nextZoom, camera.zoom)) {
            return;
        }

        camera.zoom = nextZoom;
        update();

        worldToScreen(tmpWorldBefore, tmpProjected);
        if (!tmpProjected.epsilonEquals(screenX, screenY, 0.5f)) {
            screenToWorld(screenX, screenY, tmpWorldAfter);
            focusOffset.add(tmpWorldBefore.x - tmpWorldAfter.x, tmpWorldBefore.y - tmpWorldAfter.y);
            update();
        }
    }

    public Vector2 screenToWorld(float screenX, float screenY, Vector2 out) {
        tmpVector.set(screenX, screenY, 0f);
        viewport.unproject(tmpVector);
        return out.set(tmpVector.x, tmpVector.y);
    }

    public Vector2 worldToScreen(Vector2 worldPosition, Vector2 out) {
        tmpVector.set(worldPosition.x, worldPosition.y, 0f);
        viewport.project(tmpVector);
        return out.set(tmpVector.x, tmpVector.y);
    }

    public void setFocusTarget(Vector2 target) {
        focusTarget.set(target);
    }

    public void update() {
        float visibleWidth = viewport.getWorldWidth() * camera.zoom;
        float visibleHeight = viewport.getWorldHeight() * camera.zoom;
        float halfWidth = visibleWidth * 0.5f;
        float halfHeight = visibleHeight * 0.5f;
        float desiredX = focusTarget.x + focusOffset.x;
        float desiredY = focusTarget.y + focusOffset.y;
        float cameraX = desiredX;
        float cameraY = desiredY;
        if (worldWidth > visibleWidth) {
            cameraX = MathUtils.clamp(desiredX, halfWidth, worldWidth - halfWidth);
        } else {
            cameraX = worldWidth * 0.5f;
        }
        if (worldHeight > visibleHeight) {
            cameraY = MathUtils.clamp(desiredY, halfHeight, worldHeight - halfHeight);
        } else {
            cameraY = worldHeight * 0.5f;
        }
        camera.position.set(cameraX, cameraY, 0f);
        camera.update();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public Viewport getViewport() {
        return viewport;
    }
}
