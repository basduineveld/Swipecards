package com.lorentzos.flingswipe;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Created by dionysis_lorentzos on 5/8/14
 * for package com.lorentzos.swipecards
 * and project Swipe cards.
 * Use with caution, dinosaurs might appear!
 */
public class FlingCardListener implements View.OnTouchListener {
    private static final int INVALID_POINTER_ID = -1;
    private static final int TOUCH_ABOVE = 0;
    private static final int TOUCH_BELOW = 1;
    private static final float MAX_COS = (float) Math.cos(Math.toRadians(45));

    private final View frame;
    private final Object dataObject;
    private final FlingListener mFlingListener;

    private float baseRotationDegrees;

    private float objectX = 0f;
    private float objectY = 0f;
    private int objectH = 0;
    private int objectW = 0;
    private float halfWidth = 0f;
    private int parentWidth = 0;

    private float aPosX;
    private float aPosY;
    private float aDownTouchX;
    private float aDownTouchY;

    // The active pointer is the one currently moving our object.
    private int activePointerId = INVALID_POINTER_ID;
    private int touchPosition;
    private boolean isAnimationRunning = false;

    @SuppressWarnings("UnusedDeclaration")
    public FlingCardListener(View frame, Object itemAtPosition, FlingListener flingListener) {
        this(frame, itemAtPosition, 15f, flingListener);
    }

    public FlingCardListener(View frame, Object itemAtPosition, float rotation_degrees,
                             FlingListener flingListener) {
        super();
        this.frame = frame;
        this.dataObject = itemAtPosition;
        this.baseRotationDegrees = rotation_degrees;
        this.mFlingListener = flingListener;

        if (Build.VERSION.SDK_INT >= 11) {
            setApi11Fields();
        }
    }

    @TargetApi(11)
    private void setApi11Fields() {
        this.objectX = frame.getX();
        this.objectY = frame.getY();
        this.objectH = frame.getHeight();
        this.objectW = frame.getWidth();
        this.halfWidth = objectW / 2f;
        this.parentWidth = ((ViewGroup) frame.getParent()).getWidth();
    }

    public boolean onTouch(View view, MotionEvent event) {
        //noinspection SimplifiableIfStatement
        if (Build.VERSION.SDK_INT >= 11) {
            return handleTouch(event);
        }
        return true;
    }

    @TargetApi(11)
    private boolean handleTouch(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // From
                // http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Save the ID of this pointer.
                activePointerId = event.getPointerId(0);
                final float x = event.getX(activePointerId);
                final float y = event.getY(activePointerId);

                // Remember where we started.
                aDownTouchX = x;
                aDownTouchY = y;
                // To prevent an initial jump of the magnifier, aPosX and aPosY must have the values
                // from the magnifier frame.
                if (aPosX == 0) {
                    aPosX = frame.getX();
                }
                if (aPosY == 0) {
                    aPosY = frame.getY();
                }

                if (y < objectH / 2) {
                    touchPosition = TOUCH_ABOVE;
                } else {
                    touchPosition = TOUCH_BELOW;
                }
                break;

            case MotionEvent.ACTION_UP:
                activePointerId = INVALID_POINTER_ID;
                resetCardViewOnStack();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // Extract the index of the pointer that left the touch sensor.
                final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new active pointer and adjust
                    // accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Find the index of the active pointer and fetch its position.
                final int pointerIndexMove = event.findPointerIndex(activePointerId);
                final float xMove = event.getX(pointerIndexMove);
                final float yMove = event.getY(pointerIndexMove);

                // From
                // http://android-developers.blogspot.com/2010/06/making-sense-of-multitouch.html
                // Calculate the distance moved.
                final float dx = xMove - aDownTouchX;
                final float dy = yMove - aDownTouchY;

                // Move the frame.
                aPosX += dx;
                aPosY += dy;

                // Calculate the rotation degrees.
                float distObjectX = aPosX - objectX;
                float rotation = baseRotationDegrees * 2f * distObjectX / parentWidth;
                if (touchPosition == TOUCH_BELOW) {
                    rotation = -rotation;
                }

                // In this area would be code for doing something with the view as the frame moves.
                frame.setX(aPosX);
                frame.setY(aPosY);
                frame.setRotation(rotation);
                mFlingListener.onScroll(getScrollProgressPercent());
                break;

            case MotionEvent.ACTION_CANCEL:
                activePointerId = INVALID_POINTER_ID;
                break;
        }

        return true;
    }

    @TargetApi(11)
    private float getScrollProgressPercent() {
        if (movedBeyondLeftBorder()) {
            return -1f;
        } else if (movedBeyondRightBorder()) {
            return 1f;
        } else {
            float zeroToOneValue = (aPosX + halfWidth - leftBorder()) /
                    (rightBorder() - leftBorder());
            return zeroToOneValue * 2f - 1f;
        }
    }

    @TargetApi(11)
    private boolean resetCardViewOnStack() {
        if (movedBeyondLeftBorder()) {
            // Left swipe.
            onSelected(true, getExitPoint(-objectW), 100);
            mFlingListener.onScroll(-1f);
        } else if (movedBeyondRightBorder()) {
            // Right swipe.
            onSelected(false, getExitPoint(parentWidth), 100);
            mFlingListener.onScroll(1f);
        } else {
            float absMoveDistance = Math.abs(aPosX - objectX);
            aPosX = 0;
            aPosY = 0;
            aDownTouchX = 0;
            aDownTouchY = 0;
            if (Build.VERSION.SDK_INT >= 12) {
                snapBackNew();
            } else {
                snapBackOld();
            }
            mFlingListener.onScroll(0f);
            if (absMoveDistance < 4) {
                mFlingListener.onClick(dataObject);
            }
        }
        return false;
    }

    @TargetApi(11)
    private boolean movedBeyondLeftBorder() {
        return aPosX + halfWidth < leftBorder();
    }

    @TargetApi(11)
    private boolean movedBeyondRightBorder() {
        return aPosX + halfWidth > rightBorder();
    }

    @TargetApi(11)
    private void snapBackOld() {
        frame.setX(objectX);
        frame.setY(objectY);
        frame.setRotation(0);
    }

    @TargetApi(12)
    private void snapBackNew() {
        frame.animate()
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .x(objectX)
                .y(objectY)
                .rotation(0);
    }

    @TargetApi(11)
    private float leftBorder() {
        return parentWidth / 4f;
    }

    @TargetApi(11)
    private float rightBorder() {
        return 3 * parentWidth / 4f;
    }

    @TargetApi(11)
    private void onSelected(final boolean isLeft, float exitY, long duration) {
        isAnimationRunning = true;
        float exitX;
        if (isLeft) {
            exitX = -objectW - getRotationWidthOffset();
        } else {
            exitX = parentWidth + getRotationWidthOffset();
        }
        if (Build.VERSION.SDK_INT >= 12) {
            exitNew(isLeft, exitX, exitY, duration);
        } else {
            exitOld(isLeft, exitX, exitY);
        }
    }

    @TargetApi(11)
    public void exitOld(final boolean isLeft, float exitX, float exitY) {
        this.frame.setX(exitX);
        this.frame.setY(exitY);
        if (isLeft) {
            mFlingListener.onCardExited();
            mFlingListener.leftExit(dataObject);
        } else {
            mFlingListener.onCardExited();
            mFlingListener.rightExit(dataObject);
        }
        isAnimationRunning = false;
    }

    @TargetApi(12)
    public void exitNew(final boolean isLeft, float exitX, float exitY, long duration) {
        this.frame.animate()
                .setDuration(duration)
                .setInterpolator(new AccelerateInterpolator())
                .x(exitX)
                .y(exitY)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (isLeft) {
                            mFlingListener.onCardExited();
                            mFlingListener.leftExit(dataObject);
                        } else {
                            mFlingListener.onCardExited();
                            mFlingListener.rightExit(dataObject);
                        }
                        isAnimationRunning = false;
                    }
                })
                .rotation(getExitRotation(isLeft));
    }

    /**
     * Starts a default left exit animation.
     */
    public void selectLeft() {
        if (Build.VERSION.SDK_INT >= 11) {
            if (!isAnimationRunning)
                onSelected(true, objectY, 200);
        } else {
            mFlingListener.onCardExited();
            mFlingListener.leftExit(dataObject);
        }
    }

    /**
     * Starts a default right exit animation.
     */
    public void selectRight() {
        if (Build.VERSION.SDK_INT >= 11) {
            if (!isAnimationRunning)
                onSelected(false, objectY, 200);
        } else {
            mFlingListener.onCardExited();
            mFlingListener.rightExit(dataObject);
        }
    }

    @TargetApi(11)
    private float getExitPoint(int exitXPoint) {
        float[] x = new float[2];
        x[0] = objectX;
        x[1] = aPosX;

        float[] y = new float[2];
        y[0] = objectY;
        y[1] = aPosY;

        LinearRegression regression = new LinearRegression(x, y);

        // Your typical y = ax + b linear regression.
        return (float) regression.slope() * exitXPoint + (float) regression.intercept();
    }

    @TargetApi(12)
    private float getExitRotation(boolean isLeft) {
        float rotation = baseRotationDegrees * 2f * (parentWidth - objectX) / parentWidth;
        if (touchPosition == TOUCH_BELOW) {
            rotation = -rotation;
        }
        if (isLeft) {
            rotation = -rotation;
        }
        return rotation;
    }

    /**
     * When the object rotates it's width becomes bigger.
     * The maximum width is at 45 degrees.
     * <p/>
     * The below method calculates the width offset of the rotation.
     */
    @TargetApi(11)
    private float getRotationWidthOffset() {
        return objectW / MAX_COS - objectW;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setRotationDegrees(float degrees) {
        this.baseRotationDegrees = degrees;
    }

    protected interface FlingListener {
        public void onCardExited();

        public void leftExit(Object dataObject);

        public void rightExit(Object dataObject);

        public void onClick(Object dataObject);

        @TargetApi(11)
        public void onScroll(float scrollProgressPercent);
    }
}
