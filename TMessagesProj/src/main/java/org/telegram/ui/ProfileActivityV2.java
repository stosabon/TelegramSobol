package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Property;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.AnimatedFileDrawable;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ChatActivityInterface;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.ProfileGalleryView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.VectorAvatarThumbDrawable;
import org.telegram.tgnet.TLRPC;

public class ProfileActivityV2 extends BaseFragment {

    private ProfileContainerView profileContainer;
    private ActionsContainer actionsContainer;
    private int middleStateProfileExtraHeight;
    private AvatarDrawable avatarDrawable;
    private float avatarAnimationProgress;
    private float listOffset;
    private AvatarImageView avatarImage;
    private SimpleTextView[] nameTextView = new SimpleTextView[2];
    private String nameTextViewRightDrawableContentDescription = null;
    private String nameTextViewRightDrawable2ContentDescription = null;
    private SimpleTextView[] onlineTextView = new SimpleTextView[4];
    private ActionBarMenuItem otherItem;
    private Theme.ResourcesProvider resourcesProvider;
    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private LinearLayoutManager layoutManager;
    private int rowCount;

    private long userId;
    private TLRPC.UserFull userInfo;

    public ProfileActivityV2(Bundle args) {
        this(args, null);
    }

    public static ProfileActivity of(long dialogId) {
        Bundle bundle = new Bundle();
        return new ProfileActivity(bundle);
    }

    public ProfileActivityV2(Bundle args, SharedMediaLayout.SharedMediaPreloader preloader) {
        super(args);
    }

    @Override
    public View createView(Context context) {
        Theme.createProfileResources(context);
        Theme.createChatResources(context, false);
        BaseFragment lastFragment = parentLayout.getLastFragment();
        if (lastFragment instanceof ChatActivity && ((ChatActivity) lastFragment).themeDelegate != null && ((ChatActivity) lastFragment).themeDelegate.getCurrentTheme() != null) {
            resourcesProvider = lastFragment.getResourceProvider();
        }
        middleStateProfileExtraHeight = AndroidUtilities.dp(200f);
        avatarAnimationProgress = 1f;
        listOffset = middleStateProfileExtraHeight;
        fragmentView = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                avatarAnimationProgress = (float) listOffset / middleStateProfileExtraHeight;
                final int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
                int newProfileContainerHeight = (int) (middleStateProfileExtraHeight * avatarAnimationProgress);
                profileContainer.measure(View.MeasureSpec.makeMeasureSpec(fragmentView.getMeasuredWidth(), MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(actionBarHeight + newProfileContainerHeight, MeasureSpec.EXACTLY));

                float avatarScale = AndroidUtilities.lerp(1f, (42f + 42f + 18f) / 42f, avatarAnimationProgress);
                avatarImage.measure(
                        View.MeasureSpec.makeMeasureSpec((int) (AndroidUtilities.dp(42f) * avatarScale), MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec((int) (AndroidUtilities.dp(42f) * avatarScale), MeasureSpec.EXACTLY)
                );
                avatarImage.setRoundRadius((int) (AndroidUtilities.dp(42f) * avatarScale / 2));
                for (int a = 0; a < nameTextView.length; a++) {
                    if (nameTextView[a] == null) {
                        continue;
                    }
                    nameTextView[a].measure(
                            MeasureSpec.makeMeasureSpec((int) nameTextView[a].getPaint().measureText(nameTextView[a].getText().toString() + nameTextView[a].getSideDrawablesSize()), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST)
                    );
                }
                for (int a = 0; a < onlineTextView.length; a++) {
                    if (onlineTextView[a] == null) {
                        continue;
                    }
                    onlineTextView[a].measure(
                            MeasureSpec.makeMeasureSpec((int) onlineTextView[a].getPaint().measureText(onlineTextView[a].getText().toString() + onlineTextView[a].getSideDrawablesSize()), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST)
                    );
                }
                actionsContainer.measure(
                        View.MeasureSpec.makeMeasureSpec(profileContainer.getMeasuredWidth() - AndroidUtilities.dp(16f) * 2, MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec((AndroidUtilities.dp(64f)), MeasureSpec.EXACTLY)
                );
                listView.measure(
                        View.MeasureSpec.makeMeasureSpec(fragmentView.getMeasuredWidth(), MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(fragmentView.getMeasuredHeight() - actionBarHeight, MeasureSpec.EXACTLY)
                );
                listView.setPadding(0, middleStateProfileExtraHeight,0,0);
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                final int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
                avatarAnimationProgress = (float) listOffset / middleStateProfileExtraHeight;
                int newProfileContainerHeight = (int) (middleStateProfileExtraHeight * avatarAnimationProgress);
                profileContainer.layout(0, 0, profileContainer.getMeasuredWidth(), actionBarHeight + newProfileContainerHeight);
                int avatarStartX = profileContainer.getMeasuredWidth() / 2 - avatarImage.getMeasuredWidth()/ 2;
                int avatarStartY = (int) ((actionBarHeight - AndroidUtilities.dp(30f) - ((actionBarHeight + AndroidUtilities.dp(30f)) * (1f - avatarAnimationProgress))));
                avatarImage.layout(avatarStartX, avatarStartY, avatarStartX + avatarImage.getMeasuredWidth(), avatarStartY + avatarImage.getMeasuredHeight());
                int nameMaxBottom = (int) ((actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f - 21 * AndroidUtilities.density + actionBar.getTranslationY());
                for (int a = 0; a < nameTextView.length; a++) {
                    if (nameTextView[a] == null) {
                        continue;
                    }
                    int nameWidth = nameTextView[a].getMeasuredWidth();
                    int nameStartX = profileContainer.getMeasuredWidth() / 2 - nameWidth / 2;
                    int minNameY = (int) ((actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() / 2.0f - 21 * AndroidUtilities.density + actionBar.getTranslationY());
                    int nameY = Math.max(avatarImage.getBottom(), minNameY);
                    int nameX = (int)(AndroidUtilities.lerp(AndroidUtilities.dpf2(64f), nameStartX, avatarAnimationProgress));
                    nameTextView[a].layout(
                            nameX,
                            nameY,
                            nameX + nameWidth,
                            nameY + nameTextView[a].getMeasuredHeight()
                    );
                    nameMaxBottom = Math.max(nameMaxBottom, nameY + nameTextView[a].getMeasuredHeight());
                    //nameTextView[a].setScaleX(textScale);
                    //nameTextView[a].setScaleY(textScale);
                }
                int onlineTextMaxBottom = nameMaxBottom;
                for (int a = 0; a < onlineTextView.length; a++) {
                    if (onlineTextView[a] == null) {
                        continue;
                    }
                    int onlineWidth = onlineTextView[a].getMeasuredWidth();
                    int onlineStartX = profileContainer.getMeasuredWidth() / 2 - onlineWidth / 2;
                    int onlineX = (int)(AndroidUtilities.lerp(AndroidUtilities.dpf2(64f), onlineStartX, avatarAnimationProgress));
                    onlineTextView[a].layout(
                            onlineX,
                            nameMaxBottom,
                            onlineX + onlineWidth,
                            nameMaxBottom + onlineTextView[a].getMeasuredHeight()
                    );
                    //onlineTextView[a].setScaleX(textScale);
                    //onlineTextView[a].setScaleY(textScale);
                    onlineTextMaxBottom = Math.max(onlineTextMaxBottom, nameMaxBottom + onlineTextView[a].getMeasuredHeight());
                }
                actionsContainer.layout(AndroidUtilities.dp(16f), onlineTextMaxBottom, profileContainer.getMeasuredWidth() - AndroidUtilities.dp(16f), onlineTextMaxBottom + actionsContainer.getMeasuredHeight() );
                listView.layout(0, actionBarHeight, fragmentView.getMeasuredWidth(), actionBarHeight + listView.getMeasuredHeight());
            }
        };
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        initListView(context);
        frameLayout.addView(listView);

        profileContainer = new ProfileContainerView(context);
        profileContainer.setBackgroundColor(getThemedColor(Theme.key_avatar_backgroundActionBarBlue));
        frameLayout.addView(profileContainer);
        frameLayout.addView(actionBar);
        ActionBarMenu menu = actionBar.createMenu();
        otherItem = menu.addItem(10, R.drawable.ic_ab_other, resourcesProvider);
        otherItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));
        otherItem.setIconColor(getThemedColor(Theme.key_actionBarDefaultIcon));
        initAvatar(context);
        initNameTextView(context);
        initOnlineTextView(context);
        profileContainer.addView(avatarImage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        actionsContainer = new ActionsContainer(context);
        actionsContainer.addAction(0, "Call");
        actionsContainer.addAction(0, "Notifications");
        actionsContainer.addAction(0, "Video");
        actionsContainer.addAction(0, "Block");
        profileContainer.addView(actionsContainer);
        return fragmentView;
    }

    private void initAvatar(Context context) {
        avatarDrawable = new AvatarDrawable();
        avatarDrawable.setProfile(true);
        avatarImage = new AvatarImageView(context) {
            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                if (getImageReceiver().hasNotThumb()) {
                    info.setText(LocaleController.getString(R.string.AccDescrProfilePicture));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, LocaleController.getString(R.string.Open)));
                        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, LocaleController.getString(R.string.AccDescrOpenInPhotoViewer)));
                    }
                } else {
                    info.setVisibleToUser(false);
                }
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                if (animatedEmojiDrawable != null && animatedEmojiDrawable.getImageReceiver() != null) {
                    animatedEmojiDrawable.getImageReceiver().startAnimation();
                }
            }
        };
        avatarImage.getImageReceiver().setAllowDecodeSingleFrame(true);
        avatarImage.setRoundRadius(getSmallAvatarRoundRadius());
        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user == null) {
                return;
            }
            TLRPC.FileLocation photoBig = null;
            avatarDrawable.setInfo(currentAccount, user);
            final ImageLocation imageLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_BIG);
            final ImageLocation thumbLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_SMALL);
            final ImageLocation videoThumbLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_VIDEO_BIG);
            VectorAvatarThumbDrawable vectorAvatarThumbDrawable = null;
            TLRPC.VideoSize vectorAvatar = null;
            if (userInfo != null) {
                vectorAvatar = FileLoader.getVectorMarkupVideoSize(user.photo != null && user.photo.personal ? userInfo.personal_photo : userInfo.profile_photo);
                if (vectorAvatar != null) {
                    vectorAvatarThumbDrawable = new VectorAvatarThumbDrawable(vectorAvatar, user.premium, VectorAvatarThumbDrawable.TYPE_PROFILE);
                }
            }
            final ImageLocation videoLocation = null;//avatarsViewPager.getCurrentVideoLocation(thumbLocation, imageLocation);
            //if (avatar == null) {
            //    avatarsViewPager.initIfEmpty(vectorAvatarThumbDrawable, imageLocation, thumbLocation, reload);
            //}
            if (vectorAvatar != null) {
                //avatarImage.setImageDrawable(vectorAvatarThumbDrawable);
            } else if (videoThumbLocation != null && !user.photo.personal) {
                //avatarImage.getImageReceiver().setVideoThumbIsSame(true);
                //avatarImage.setImage(videoThumbLocation, "avatar", thumbLocation, "50_50", avatarDrawable, user);
            } else {
                avatarImage.setImage(videoLocation, ImageLoader.AUTOPLAY_FILTER, thumbLocation, "50_50", avatarDrawable, user);
            }
        }
    }

    private int getSmallAvatarRoundRadius() {
        //if (chatId != 0) {
        //    TLRPC.Chat chatLocal = getMessagesController().getChat(chatId);
        //    if (ChatObject.isForum(chatLocal)) {
        //        return AndroidUtilities.dp(needInsetForStories() ? 11 : 16);
        //    }
        //}
        return AndroidUtilities.dp(21);
    }

    private void initNameTextView(Context context) {
        for (int a = 0; a < nameTextView.length; a++) {
            nameTextView[a] = new SimpleTextView(context) {
                @Override
                public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(info);
                    if (isFocusable() && (nameTextViewRightDrawableContentDescription != null || nameTextViewRightDrawable2ContentDescription != null)) {
                        StringBuilder s = new StringBuilder(getText());
                        if (nameTextViewRightDrawable2ContentDescription != null) {
                            if (s.length() > 0) s.append(", ");
                            s.append(nameTextViewRightDrawable2ContentDescription);
                        }
                        if (nameTextViewRightDrawableContentDescription != null) {
                            if (s.length() > 0) s.append(", ");
                            s.append(nameTextViewRightDrawableContentDescription);
                        }
                        info.setText(s);
                    }
                }
            };
            if (a == 1) {
                nameTextView[a].setTextColor(getThemedColor(Theme.key_profile_title));
            } else {
                nameTextView[a].setTextColor(getThemedColor(Theme.key_actionBarDefaultTitle));
            }
            nameTextView[a].setPadding(0, AndroidUtilities.dp(6), 0, 0);
            nameTextView[a].setTextSize(18);
            nameTextView[a].setGravity(Gravity.LEFT);
            nameTextView[a].setTypeface(AndroidUtilities.bold());
            nameTextView[a].setLeftDrawableTopPadding(-AndroidUtilities.dp(1.3f));
            nameTextView[a].setPivotX(0);
            nameTextView[a].setPivotY(0);
            nameTextView[a].setAlpha(a == 0 ? 0.0f : 1.0f);
            if (a == 1) {
                nameTextView[a].setScrollNonFitText(true);
                nameTextView[a].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            nameTextView[a].setFocusable(a == 0);
            nameTextView[a].setEllipsizeByGradient(true);
            nameTextView[a].setRightDrawableOutside(a == 0);
            nameTextView[a].setText("Text " + a);
            profileContainer.addView(nameTextView[a], LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }
    }

    private void initOnlineTextView(Context context) {
        for (int a = 0; a < onlineTextView.length; a++) {
            if (a == 1) {
                onlineTextView[a] = new LinkSpanDrawable.ClickableSmallTextView(context) {
                };
            } else {
                onlineTextView[a] = new LinkSpanDrawable.ClickableSmallTextView(context);
            }

            onlineTextView[a].setEllipsizeByGradient(true);
            onlineTextView[a].setTextColor(getThemedColor(Theme.key_avatar_subtitleInProfileBlue));
            onlineTextView[a].setTextSize(14);
            onlineTextView[a].setGravity(Gravity.LEFT);
            onlineTextView[a].setAlpha(a == 0 ? 0.0f : 1.0f);
            if (a == 1 || a == 2 || a == 3) {
                //onlineTextView[a].setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(2));
            }
            if (a > 0) {
                onlineTextView[a].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            onlineTextView[a].setFocusable(a == 0);
            onlineTextView[a].setText("online");
            profileContainer.addView(onlineTextView[a], LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        }
    }

    private void initListView(Context context) {
        listView = new RecyclerListView(context);
        layoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(layoutManager);
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        listView.setClipToPadding(false);
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int newOffset = 0;
                View child = null;
                for (int i = 0; i < listView.getChildCount(); i++) {
                    if (listView.getChildAdapterPosition(listView.getChildAt(i)) == 0) {
                        child = listView.getChildAt(i);
                        break;
                    }
                }
                RecyclerListView.Holder holder = child == null ? null : (RecyclerListView.Holder) listView.findContainingViewHolder(child);
                int top = child == null ? 0 : child.getTop();
                int adapterPosition = holder != null ? holder.getAdapterPosition() : RecyclerView.NO_POSITION;
                if (top >= 0 && adapterPosition == 0) {
                    newOffset = top;
                }
                if (listOffset != newOffset) {
                    listOffset = newOffset;
                    profileContainer.requestLayout();
                }
            }
        });
    }

    @Override
    public ActionBar createActionBar(Context context) {
        BaseFragment lastFragment = parentLayout.getLastFragment();
        if (lastFragment instanceof ChatActivity && ((ChatActivity) lastFragment).themeDelegate != null && ((ChatActivity) lastFragment).themeDelegate.getCurrentTheme() != null) {
            resourcesProvider = lastFragment.getResourceProvider();
        }
        ActionBar actionBar = new ActionBar(context, resourcesProvider);
        actionBar.setForceSkipTouches(true);
        actionBar.setBackgroundColor(Color.TRANSPARENT);
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarDefaultIcon), false);
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarDefaultIcon), true);
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);
        actionBar.setClipContent(true);
        actionBar.setOccupyStatusBar(Build.VERSION.SDK_INT >= 21 && !AndroidUtilities.isTablet() && !inBubbleMode);
        return actionBar;
    }

    public boolean onFragmentCreate() {
        userId = arguments.getLong("user_id", 0);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (avatarImage != null) {
            avatarImage.setImageDrawable(null);
        }
    }

    private class ProfileContainerView extends FrameLayout {

        public ProfileContainerView(Context context) {
            super(context);
        }
    }

    public static class AvatarImageView extends BackupImageView {

        public AvatarImageView(Context context) {
            super(context);
        }
    }

    public static class ActionsView extends FrameLayout {

        public ActionsView(Context context) {
            super(context);
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = new HeaderCell(mContext, 23, resourcesProvider);
            view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            HeaderCell headerCell = (HeaderCell) holder.itemView;
            headerCell.setText(position + "");
            headerCell.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        }

        @Override
        public int getItemCount() {
            return 30;
        }
    }

    private class ActionsContainer extends ViewGroup {

        public ActionsContainer(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int childCount = getChildCount();
            if (childCount == 0) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }

            int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
            int horizontalPadding = getPaddingLeft() + getPaddingRight();
            int availableWidth = totalWidth - horizontalPadding;
            int childWidth = availableWidth / childCount;

            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                measureChild(
                        child,
                        MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY)
                );
            }

            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int childCount = getChildCount();
            if (childCount == 0) return;

            int x = 0;

            int totalWidth = r - l;
            int itemSpace = AndroidUtilities.dp(8f);
            int horizontalPadding = (childCount - 1) * itemSpace;
            int availableWidth = totalWidth - horizontalPadding;
            int childWidth = availableWidth / childCount;

            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) continue;
                // Account for margins in layout, but do not add extra spacing between items
                int childLeft = x;
                int childRight = childLeft + childWidth;

                child.layout(childLeft, 0, childRight, getMeasuredHeight());
                x += childWidth + itemSpace;
            }
        }

        // Add action items programmatically
        public void addAction(int iconResId, String text) {
            View actionView = createActionView(iconResId, text);
            addView(actionView);
        }

        private View createActionView(int drawableResId, String text) {
            Context context = getContext();
            LinearLayout container = new LinearLayout(context);
            container.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER);

            ImageView icon = new ImageView(context);
            icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_ab_other));
            icon.setAdjustViewBounds(true);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            TextView label = new TextView(context);
            label.setText(text);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            label.setEllipsize(TextUtils.TruncateAt.END);
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setColor(Color.argb(38, 0, 0, 0));
            background.setCornerRadius(AndroidUtilities.dp(12f));
            container.setBackground(background);

            container.addView(icon, new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
            ));

            container.addView(label, new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
            ));

            return container;
        }
    }

}
