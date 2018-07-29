package com.example.joaquinc.imageviewanimator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.animation.FlingAnimation;
import android.support.animation.FloatValueHolder;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rx.Observable;
import rx.subjects.PublishSubject;

public class SwipeView extends FrameLayout {

    private GestureDetector gestureDetector;
    private Unbinder unbinder;
    private ValueAnimator animator;
    private FlingAnimation flingAnimation;
    private boolean triggered = false;

    private PublishSubject<Float> progressSubject = PublishSubject.create();
    private PublishSubject<Void> completeSubject = PublishSubject.create();

    public static final float FLING_FRICTION = .85f;
    public static final double THRESHOLD_FRACTION = .90;
    public static final int ANIMATE_TO_START_DURATION = 200;
    public static final int ANIMATE_TO_END_DURATION = 200;

    private float previousLocation = 0;

    @BindView(R.id.swipe_layout) FrameLayout layout;
    @BindView(R.id.image_layout) ConstraintLayout imageLayout;
    @BindView(R.id.nope_text) FrameLayout nopeText;

    private void init() {
        View view = inflate(getContext(), R.layout.swipe_layout, this);
        unbinder = ButterKnife.bind(view);
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                cancelAnimations();
                if(previousLocation == 0) {
                    previousLocation = motionEvent1.getX();
                    return true;
                }
                float calculateDifference = motionEvent1.getX() - previousLocation;
                Log.e("calculateDifference", "calculateDifference: " + calculateDifference);
                if(calculateDifference < 0) {
                    Log.e("SWIPE LEFT", "SWIPE LEFT:" + calculateDifference);
                    cancelAnimations();
                    return true;
                }else {
                    Log.e("SWIPE RIGHT", "SWIPE RIGHT:" + calculateDifference);
                    cancelAnimations();
                    setDragProgress(calculateDifference);
                }

                return true;
            }

            @Override
            public boolean onFling(MotionEvent downEvent, MotionEvent moveEvent, float velocityX, float velocityY) {
                if(velocityX < 0) {
                    return false;
                }
                cancelAnimations();
                flingAnimation = new FlingAnimation(new FloatValueHolder(moveEvent.getX()));
                flingAnimation.setStartVelocity(velocityX)
                        .setMaxValue(layout.getWidth() + 5000)
                        .setFriction(FLING_FRICTION)
                        .addUpdateListener((dynamicAnimation, val, velocity) -> setDragProgress(val))
                        .addEndListener((dynamicAnimation, canceled, val, velocity) -> onDragFinished(val))
                        .start();

                return true;
            }
        });
        gestureDetector.setIsLongpressEnabled(false);
    }

    private void setDragProgress(float x) {
        setPadding((int)x, 0, - (int)x, 0);
        if(!triggered) {
            nopeText.setAlpha(x / getWidth());
            nopeText.requestLayout();

        } else {
            nopeText.setAlpha(1);
            nopeText.requestLayout();
        }
        imageLayout.requestLayout();
        layout.requestLayout();
    }

    private void onDragFinished(float finalX) {
        if(finalX > THRESHOLD_FRACTION * (layout.getWidth() + 300)) {
            animateToEnd(finalX);
        } else {
            animateToStart();
        }
    }

    private void animateToStart() {
        cancelAnimations();
        animator = ValueAnimator.ofFloat(getPaddingLeft(), 0);
        animator.addUpdateListener(valueAnimator -> setDragProgress((Float)valueAnimator.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if(triggered) {
                    completeSubject.onNext(null);
                }
            }
        });
        animator.setDuration(ANIMATE_TO_START_DURATION);
        animator.start();
    }

    private void animateToEnd(float currentValue) {
        cancelAnimations();
        float rightEdge = layout.getWidth() + imageLayout.getX();
        animator = ValueAnimator.ofFloat(currentValue, rightEdge * 2);
        animator.addUpdateListener(valueAnimator -> setDragProgress((Float)valueAnimator.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                triggered = true;
            }
        });
        animator.setDuration(ANIMATE_TO_END_DURATION);
        animator.start();
    }

    private void cancelAnimations() {
        if(animator != null) {
            animator.cancel();
        }
        if(flingAnimation != null) {
            flingAnimation.cancel();
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if(triggered) {
            return true;
        }
        if(gestureDetector.onTouchEvent(event)) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                onDragFinished(event.getX());
                break;
        }

        return true;
    }

    public Observable<Float> getProgressObservable() {
        return progressSubject.asObservable();
    }

    public Observable<Void> getCompleteObservable() {
        return completeSubject.asObservable();
    }

    public SwipeView(@NonNull Context context) {
        super(context);
        init();
    }

    public SwipeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwipeView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwipeView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }
}
