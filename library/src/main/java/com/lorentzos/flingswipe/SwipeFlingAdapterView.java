package com.lorentzos.flingswipe;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Adapter;
import android.widget.FrameLayout;

/**
 * Created by dionysis_lorentzos on 5/8/14
 * for package com.lorentzos.swipecards
 * and project Swipe cards.
 * Use with caution, dinosaurs might appear!
 */
public class SwipeFlingAdapterView extends BaseFlingAdapterView {
    private final int MAX_VISIBLE;
    private final float ROTATION_DEGREES;

    private Adapter adapter;
    private int lastObjectInStack = 0;
    private onFlingListener flingListener;
    private AdapterDataSetObserver dataSetObserver;
    private boolean inLayout = false;
    private View activeCard = null;
    private OnItemClickListener onItemClickListener;
    private FlingCardListener flingCardListener;

    public SwipeFlingAdapterView(Context context) {
        this(context, null);
    }

    public SwipeFlingAdapterView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.SwipeFlingStyle);
    }

    public SwipeFlingAdapterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SwipeFlingAdapterView,
                defStyle, 0);
        MAX_VISIBLE = a.getInt(R.styleable.SwipeFlingAdapterView_max_visible, 4);
        ROTATION_DEGREES = a.getFloat(R.styleable.SwipeFlingAdapterView_rotation_degrees, 15f);
        a.recycle();
    }

    /**
     * A shortcut method to set both the listeners and the adapter.
     *
     * @param context  The activity context which extends onFlingListener, OnItemClickListener or
     *                 both.
     * @param mAdapter The adapter you have to set.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void init(final Context context, Adapter mAdapter) {
        if (context instanceof onFlingListener) {
            flingListener = (onFlingListener) context;
        } else {
            throw new RuntimeException("Activity does not implement " +
                    "SwipeFlingAdapterView.onFlingListener");
        }
        if (context instanceof OnItemClickListener) {
            onItemClickListener = (OnItemClickListener) context;
        }
        setAdapter(mAdapter);
    }

    @Override
    public View getSelectedView() {
        return activeCard;
    }

    @Override
    public void requestLayout() {
        if (!inLayout) {
            super.requestLayout();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // If we don't have an adapter, we don't need to do anything.
        if (adapter == null) {
            return;
        }

        inLayout = true;
        final int adapterCount = adapter.getCount();

        removeAllViewsInLayout();
        if (adapterCount > 0) {
            layoutChildren(adapterCount);
            setTopView();
        }
        inLayout = false;

        if (adapterCount < MAX_VISIBLE) {
            flingListener.onAdapterAboutToEmpty(adapterCount);
        }
    }

    private void layoutChildren(int adapterCount) {
        for (int i = 0; i < Math.min(adapterCount, MAX_VISIBLE); i++) {
            View newUnderChild = adapter.getView(i, null, this);
            if (newUnderChild.getVisibility() != GONE) {
                makeAndAddView(newUnderChild);
                lastObjectInStack = i;
            }
        }
    }

    private void makeAndAddView(View child) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
        addViewInLayout(child, 0, lp, true);

        final boolean needToMeasure = child.isLayoutRequested();
        if (needToMeasure) {
            int childWidthSpec = getChildMeasureSpec(getWidthMeasureSpec(),
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
            int childHeightSpec = getChildMeasureSpec(getHeightMeasureSpec(),
                    getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
                    lp.height);
            child.measure(childWidthSpec, childHeightSpec);
        } else {
            cleanupLayoutState(child);
        }

        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();

        int gravity = lp.gravity;
        if (gravity == -1) {
            gravity = Gravity.TOP | Gravity.LEFT;
        }

        final int horizontalGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

        int childLeft;
        int childTop;
        switch (horizontalGravity) {
            case Gravity.CENTER_HORIZONTAL:
                childLeft = (getWidth() + getPaddingLeft() - getPaddingRight() - w) / 2 +
                        lp.leftMargin - lp.rightMargin;
                break;
            case Gravity.END:
                childLeft = getWidth() + getPaddingRight() - w - lp.rightMargin;
                break;
            case Gravity.START:
            default:
                childLeft = getPaddingLeft() + lp.leftMargin;
                break;
        }
        switch (verticalGravity) {
            case Gravity.CENTER_VERTICAL:
                childTop = (getHeight() + getPaddingTop() - getPaddingBottom() - h) / 2 +
                        lp.topMargin - lp.bottomMargin;
                break;
            case Gravity.BOTTOM:
                childTop = getHeight() - getPaddingBottom() - h - lp.bottomMargin;
                break;
            case Gravity.TOP:
            default:
                childTop = getPaddingTop() + lp.topMargin;
                break;
        }

        child.layout(childLeft, childTop, childLeft + w, childTop + h);
    }

    /**
     * Set the top view and add the fling listener.
     */
    private void setTopView() {
        if (getChildCount() > 0) {

            activeCard = getChildAt(lastObjectInStack);
            if (activeCard != null) {

                flingCardListener = new FlingCardListener(activeCard, adapter.getItem(0),
                        ROTATION_DEGREES, new FlingCardListener.FlingListener() {

                    @Override
                    public void onCardExited() {
                        activeCard = null;
                        flingListener.removeFirstObjectInAdapter();
                    }

                    @Override
                    public void leftExit(Object dataObject) {
                        flingListener.onLeftCardExit(dataObject);
                    }

                    @Override
                    public void rightExit(Object dataObject) {
                        flingListener.onRightCardExit(dataObject);
                    }

                    @Override
                    public void onClick(Object dataObject) {
                        if (onItemClickListener != null) {
                            onItemClickListener.onItemClicked(0, dataObject);
                        }
                    }

                    @Override
                    public void onScroll(float scrollProgressPercent) {
                        flingListener.onScroll(scrollProgressPercent);
                    }
                });

                activeCard.setOnTouchListener(flingCardListener);
            }
        }
    }

    public FlingCardListener getTopCardListener() throws NullPointerException {
        if (flingCardListener == null) {
            throw new NullPointerException();
        }
        return flingCardListener;
    }

    @Override
    public Adapter getAdapter() {
        return adapter;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (this.adapter != null && dataSetObserver != null) {
            this.adapter.unregisterDataSetObserver(dataSetObserver);
            dataSetObserver = null;
        }

        this.adapter = adapter;

        if (this.adapter != null && dataSetObserver == null) {
            dataSetObserver = new AdapterDataSetObserver();
            this.adapter.registerDataSetObserver(dataSetObserver);
        }
    }

    public void setFlingListener(onFlingListener onFlingListener) {
        this.flingListener = onFlingListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new FrameLayout.LayoutParams(getContext(), attrs);
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            requestLayout();
        }
    }

    public interface OnItemClickListener {
        public void onItemClicked(int itemPosition, Object dataObject);
    }

    public interface onFlingListener {
        public void removeFirstObjectInAdapter();

        public void onLeftCardExit(Object dataObject);

        public void onRightCardExit(Object dataObject);

        public void onAdapterAboutToEmpty(int itemsInAdapter);

        @TargetApi(11)
        public void onScroll(float scrollProgressPercent);
    }
}
