package com.reactnativenavigation.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;

import com.reactnativenavigation.params.BaseTitleBarButtonParams;
import com.reactnativenavigation.params.StyleParams;
import com.reactnativenavigation.params.TitleBarButtonParams;
import com.reactnativenavigation.params.TitleBarLeftButtonParams;
import com.reactnativenavigation.utils.ViewUtils;

import java.util.List;

public class TitleBar extends Toolbar {
    private static final int TITLE_VISIBILITY_ANIMATION_DURATION = 320;
    private LeftButton leftButton;
    private ActionMenuView actionMenuView;
    private boolean titleBarTitleTextCentered;

    public TitleBar(Context context) {
        super(context);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (titleBarTitleTextCentered) {
            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point screenSize = new Point();
            display.getSize(screenSize);

            int[] location = new int[2];
            getTitleView().getLocationOnScreen(location);
            getTitleView().setTranslationX(getTitleView().getTranslationX() + (-location[0] + screenSize.x / 2 - getTitleView().getWidth() / 2));
        }
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child instanceof ActionMenuView) {
            actionMenuView = (ActionMenuView) child;
        }
    }

    public void setRightButtons(List<TitleBarButtonParams> rightButtons, String navigatorEventId) {
        Menu menu = getMenu();
        menu.clear();
        if (rightButtons == null) {
            return;
        }
        addButtonsToTitleBar(rightButtons, navigatorEventId, menu);
    }

    public void setLeftButton(TitleBarLeftButtonParams leftButtonParams,
                              LeftButtonOnClickListener leftButtonOnClickListener,
                              String navigatorEventId,
                              boolean overrideBackPressInJs) {
        if (shouldSetLeftButton(leftButtonParams)) {
            createAndSetLeftButton(leftButtonParams, leftButtonOnClickListener, navigatorEventId, overrideBackPressInJs);
        } else if (hasLeftButton()) {
            if (leftButtonParams.hasIcon()) {
                updateLeftButton(leftButtonParams);
            } else {
                removeLeftButton();
            }
        }
    }

    private void removeLeftButton() {
        setNavigationIcon(null);
        leftButton = null;
    }

    public void setStyle(StyleParams params) {
        titleBarTitleTextCentered = params.titleBarTitleTextCentered;
        setVisibility(params.titleBarHidden ? GONE : VISIBLE);
        setTitleTextColor(params);
        setTitleTextFont(params);
        setSubtitleTextColor(params);
        colorOverflowButton(params);
        setBackground(params);
    }

    private void colorOverflowButton(StyleParams params) {
        Drawable overflowIcon = actionMenuView.getOverflowIcon();
        if (shouldColorOverflowButton(params, overflowIcon)) {
            ViewUtils.tintDrawable(overflowIcon, params.titleBarButtonColor.getColor(), true);
        }
    }

    protected void setBackground(StyleParams params) {
        setTranslucent(params);
    }

    protected void setTranslucent(StyleParams params) {
        if (params.topBarTranslucent) {
            setBackground(new TranslucentDrawable());
        }
    }

    private boolean shouldColorOverflowButton(StyleParams params, Drawable overflowIcon) {
        return overflowIcon != null && params.titleBarButtonColor.hasColor();
    }

    protected void setTitleTextColor(StyleParams params) {
        if (params.titleBarTitleColor.hasColor()) {
            setTitleTextColor(params.titleBarTitleColor.getColor());
        }
    }

    protected void setTitleTextFont(StyleParams params) {
        if (params.titleBarTitleFont == null || params.titleBarTitleFont.isEmpty()) {
            return;
        }

        View titleView = getTitleView();

        if (titleView == null || !(titleView instanceof TextView)) {
            return;
        }

        Typeface typeface = null;

        try {
            typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + params.titleBarTitleFont + ".ttf");
        } catch (RuntimeException re) {
            try {
                typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + params.titleBarTitleFont + ".otf");
            } catch (RuntimeException re2) {
                return;
            }
        }
        ((TextView) titleView).setTypeface(typeface);
    }

    protected void setSubtitleTextColor(StyleParams params) {
        if (params.titleBarSubtitleColor.hasColor()) {
            setSubtitleTextColor(params.titleBarSubtitleColor.getColor());
        }
    }

    private void addButtonsToTitleBar(List<TitleBarButtonParams> rightButtons, String navigatorEventId, Menu menu) {
        for (int i = 0; i < rightButtons.size(); i++) {
            final TitleBarButton button = ButtonFactory.create(menu, this, rightButtons.get(i), navigatorEventId);
            addButtonInReverseOrder(rightButtons, i, button);
        }
    }

    protected void addButtonInReverseOrder(List<? extends BaseTitleBarButtonParams> buttons, int i, TitleBarButton button) {
        final int index = buttons.size() - i - 1;
        button.addToMenu(index);
    }

    private boolean hasLeftButton() {
        return leftButton != null;
    }

    private void updateLeftButton(TitleBarLeftButtonParams leftButtonParams) {
        leftButton.setIconState(leftButtonParams);
    }

    private boolean shouldSetLeftButton(TitleBarLeftButtonParams leftButtonParams) {
        return leftButton == null && leftButtonParams != null && leftButtonParams.iconState != null;
    }

    private void createAndSetLeftButton(TitleBarLeftButtonParams leftButtonParams,
                                        LeftButtonOnClickListener leftButtonOnClickListener,
                                        String navigatorEventId,
                                        boolean overrideBackPressInJs) {
        leftButton = new LeftButton(getContext(), leftButtonParams, leftButtonOnClickListener, navigatorEventId,
                overrideBackPressInJs);
        setNavigationOnClickListener(leftButton);

        if (leftButtonParams.icon != null) {
            setNavigationIcon(leftButtonParams.icon);
        } else {
            setNavigationIcon(leftButton);
        }
    }

    public void hide() {
        hide(null);
    }

    public void hide(@Nullable final Runnable onHidden) {
        animate()
                .alpha(0)
                .setDuration(200)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (onHidden != null) {
                            onHidden.run();
                        }
                    }
                });
    }

    public void show() {
        this.show(null);
    }

    public void show(final @Nullable Runnable onDisplayed) {
        setAlpha(0);
        animate()
                .alpha(1)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (onDisplayed != null) {
                            onDisplayed.run();
                        }
                    }
                });
    }

    public void showTitle() {
        animateTitle(1);
    }

    public void hideTitle() {
        animateTitle(0);
    }

    private void animateTitle(int alpha) {
        View titleView = getTitleView();
        if (titleView != null) {
            titleView.animate()
                    .alpha(alpha)
                    .setDuration(TITLE_VISIBILITY_ANIMATION_DURATION);
        }
    }

    @Nullable
    protected View getTitleView() {
        return ViewUtils.findChildByClass(this, TextView.class, new ViewUtils.Matcher<TextView>() {
            @Override
            public boolean match(TextView child) {
                return child.getText().equals(getTitle());
            }
        });
    }
}