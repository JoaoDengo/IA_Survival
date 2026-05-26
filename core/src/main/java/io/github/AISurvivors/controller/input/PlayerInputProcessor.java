package io.github.AISurvivors.controller.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;

public class PlayerInputProcessor implements InputProcessor {
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean resetCameraRequested;
    private boolean debugHitboxToggleRequested;
    private boolean cameraDragActive;
    private boolean cameraDragPending;
    private boolean worldClickPending;
    private float pendingZoomAmount;
    private final Vector2 worldClickScreenPosition = new Vector2();
    private final Vector2 zoomScreenPosition = new Vector2();
    private final Vector2 dragPreviousScreenPosition = new Vector2();
    private final Vector2 dragCurrentScreenPosition = new Vector2();
    private final Vector2 lastDragScreenPosition = new Vector2();

    public Vector2 getMovementDirection(Vector2 outDirection) {
        outDirection.setZero();

        if (upPressed) {
            outDirection.y += 1f;
        }
        if (downPressed) {
            outDirection.y -= 1f;
        }
        if (leftPressed) {
            outDirection.x -= 1f;
        }
        if (rightPressed) {
            outDirection.x += 1f;
        }

        if (!outDirection.isZero()) {
            outDirection.nor();
        }

        return outDirection;
    }

    public boolean consumeCameraReset() {
        boolean consumed = resetCameraRequested;
        resetCameraRequested = false;
        return consumed;
    }

    public boolean consumeDebugHitboxToggle() {
        boolean consumed = debugHitboxToggleRequested;
        debugHitboxToggleRequested = false;
        return consumed;
    }

    public boolean consumeWorldClick(Vector2 outScreenPosition) {
        if (!worldClickPending) {
            return false;
        }

        outScreenPosition.set(worldClickScreenPosition);
        worldClickPending = false;
        return true;
    }

    public float consumeZoomAmount(Vector2 outScreenPosition) {
        float zoomAmount = pendingZoomAmount;
        if (zoomAmount == 0f) {
            return 0f;
        }

        outScreenPosition.set(zoomScreenPosition);
        pendingZoomAmount = 0f;
        return zoomAmount;
    }

    public boolean consumeCameraDrag(Vector2 outPreviousScreenPosition, Vector2 outCurrentScreenPosition) {
        if (!cameraDragPending) {
            return false;
        }

        outPreviousScreenPosition.set(dragPreviousScreenPosition);
        outCurrentScreenPosition.set(dragCurrentScreenPosition);
        cameraDragPending = false;
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.W:
            case Input.Keys.UP:
                upPressed = true;
                return true;
            case Input.Keys.S:
            case Input.Keys.DOWN:
                downPressed = true;
                return true;
            case Input.Keys.A:
            case Input.Keys.LEFT:
                leftPressed = true;
                return true;
            case Input.Keys.D:
            case Input.Keys.RIGHT:
                rightPressed = true;
                return true;
            case Input.Keys.SPACE:
                resetCameraRequested = true;
                return true;
            case Input.Keys.H:
                debugHitboxToggleRequested = true;
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.W:
            case Input.Keys.UP:
                upPressed = false;
                return true;
            case Input.Keys.S:
            case Input.Keys.DOWN:
                downPressed = false;
                return true;
            case Input.Keys.A:
            case Input.Keys.LEFT:
                leftPressed = false;
                return true;
            case Input.Keys.D:
            case Input.Keys.RIGHT:
                rightPressed = false;
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT) {
            worldClickScreenPosition.set(screenX, screenY);
            worldClickPending = true;
            return true;
        }

        if (button == Input.Buttons.RIGHT) {
            cameraDragActive = true;
            lastDragScreenPosition.set(screenX, screenY);
            dragPreviousScreenPosition.set(screenX, screenY);
            dragCurrentScreenPosition.set(screenX, screenY);
            cameraDragPending = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.RIGHT) {
            cameraDragActive = false;
            cameraDragPending = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!cameraDragActive) {
            return false;
        }

        dragPreviousScreenPosition.set(lastDragScreenPosition);
        dragCurrentScreenPosition.set(screenX, screenY);
        lastDragScreenPosition.set(screenX, screenY);
        cameraDragPending = true;
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        pendingZoomAmount += amountY;
        zoomScreenPosition.set(Gdx.input.getX(), Gdx.input.getY());
        return true;
    }
}
