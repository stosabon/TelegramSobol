package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Property;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

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
import org.telegram.ui.Components.ProfileGalleryView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.VectorAvatarThumbDrawable;
import org.telegram.tgnet.TLRPC;

public class ProfileActivityV2 extends BaseFragment {

    private ProfileContainerView profileContainer;
    private int middleStateProfileExtraHeight;
    private AvatarDrawable avatarDrawable;
    private float avatarAnimationProgress;
    private AvatarImageView avatarImage;
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
        middleStateProfileExtraHeight = AndroidUtilities.dp(128f);
        avatarAnimationProgress = 1f;
        fragmentView = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                final int actionBarHeight = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
                int newProfileContainerHeight = (int) (middleStateProfileExtraHeight * avatarAnimationProgress);
                profileContainer.measure(View.MeasureSpec.makeMeasureSpec(fragmentView.getMeasuredWidth(), MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(actionBarHeight + newProfileContainerHeight, MeasureSpec.EXACTLY));

                float avatarScale = AndroidUtilities.lerp(1f, (42f + 42f + 18f) / 42f, avatarAnimationProgress);
                Log.e("STAS", "avatarScale " + avatarScale);
                avatarImage.measure(
                        View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(42f), MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(42f), MeasureSpec.EXACTLY)
                );
                avatarImage.setScaleX(avatarScale);
                avatarImage.setScaleY(avatarScale);
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
                int newProfileContainerHeight = (int) (middleStateProfileExtraHeight * avatarAnimationProgress);
                profileContainer.layout(0, 0, profileContainer.getMeasuredWidth(), actionBarHeight + newProfileContainerHeight);
                int avatarStartX = profileContainer.getMeasuredWidth() / 2 - avatarImage.getMeasuredWidth()/ 2;
                int avatarStartY = (int) (actionBarHeight * avatarAnimationProgress);
                avatarImage.layout(avatarStartX, avatarStartY, avatarStartX + avatarImage.getMeasuredWidth(), avatarStartY + avatarImage.getMeasuredHeight());
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
        profileContainer.addView(avatarImage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
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
                float newAvatarAnimationProgress = (float) newOffset / middleStateProfileExtraHeight;
                if (avatarAnimationProgress != newAvatarAnimationProgress) {
                    avatarAnimationProgress = newAvatarAnimationProgress;
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
}
