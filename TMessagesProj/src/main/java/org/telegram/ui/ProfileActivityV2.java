package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.util.SparseIntArray;
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
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Business.ProfileHoursCell;
import org.telegram.ui.Cells.AboutLinkCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ProfileChannelCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimatedColor;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ChatActivityInterface;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CrossfadeDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.DotDividerSpan;
import org.telegram.ui.Components.EmptyStubSpan;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.MessagePrivateSeenView;
import org.telegram.ui.Components.Premium.PremiumPreviewBottomSheet;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScamDrawable;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.VectorAvatarThumbDrawable;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.voip.VoIPHelper;
import org.telegram.ui.Stars.ProfileGiftsViewV2;
import org.telegram.ui.Stars.StarGiftPatterns;
import org.telegram.ui.Stars.StarsController;

import java.util.concurrent.CountDownLatch;

public class ProfileActivityV2 extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private SizeNotifierFrameLayout contentView;

    private ProfileContainerView profileContainer;
    public ProfileGiftsViewV2 giftsView;
    private AvatarImageView avatarImage;
    private AvatarDrawable avatarDrawable;
    private float avatarAnimationProgress;
    private float listOffset;
    private ActionsContainer actionsContainer;
    private int middleStateProfileExtraHeight;
    private float currentExtraHeight;
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
    private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[] emojiStatusDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable[2];
    private final CrossfadeDrawable[] premiumCrossfadeDrawable = new CrossfadeDrawable[2];
    private final Drawable[] premiumStarDrawable = new Drawable[2];
    private Drawable lockIconDrawable;
    private Long emojiStatusGiftId;

    private boolean fragmentViewAttached; /** READY */

    private boolean messageActionVisible; /** Double check discuss text */
    private boolean notificationsActionVisible; /** READY */
    private boolean callActionVisible;
    private boolean videoCallActionVisible;
    private boolean giftActionVisible;
    private boolean shareActionVisible;
    private boolean joinActionVisible;
    private boolean leaveActionVisible; /** READY */

    private long chatId;
    private long userId;
    private long dialogId;
    private boolean isTopic;
    private boolean isBot;
    private long topicId;
    private boolean userBlocked;
    private int onlineCount = -1;
    private TLRPC.UserFull userInfo;
    private TLRPC.ChatFull chatInfo;
    private TLRPC.Chat currentChat;
    private TLRPC.EncryptedChat currentEncryptedChat;

    private boolean hasFallbackPhoto;
    private boolean[] isOnline = new boolean[1];
    private int actionBarBackgroundColor;
    private MessagesController.PeerColor peerColor;
    private final SparseIntArray adaptedColors = new SparseIntArray();
    protected float headerShadowAlpha = 1.0f;

    BaseFragment previousTransitionMainFragment;
    ChatActivityInterface previousTransitionFragment;
    private ImageLocation prevLoadedImageLocation;

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
        middleStateProfileExtraHeight = AndroidUtilities.dp(210f);
        currentExtraHeight = AndroidUtilities.dp(88f);
        avatarAnimationProgress = 1f;
        listOffset = middleStateProfileExtraHeight;
        fragmentView = new SizeNotifierFrameLayout(context) {

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                fragmentViewAttached = true;
                for (int i = 0; i < emojiStatusDrawable.length; i++) {
                    if (emojiStatusDrawable[i] != null) {
                        emojiStatusDrawable[i].attach();
                    }
                }
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                fragmentViewAttached = false;
                for (int i = 0; i < emojiStatusDrawable.length; i++) {
                    if (emojiStatusDrawable[i] != null) {
                        emojiStatusDrawable[i].detach();
                    }
                }
            }

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
                    onlineTextMaxBottom = Math.max(onlineTextMaxBottom, nameMaxBottom + onlineTextView[a].getMeasuredHeight());
                }
                actionsContainer.layout(AndroidUtilities.dp(16f), onlineTextMaxBottom + AndroidUtilities.dp(16f), profileContainer.getMeasuredWidth() - AndroidUtilities.dp(16f), onlineTextMaxBottom + actionsContainer.getMeasuredHeight() + AndroidUtilities.dp(16f) );
                listView.layout(0, actionBarHeight, fragmentView.getMeasuredWidth(), actionBarHeight + listView.getMeasuredHeight());
                if (giftsView != null) {
                    giftsView.setExpandCoords(
                            profileContainer.getMeasuredWidth() - AndroidUtilities.dp(40),
                            false,
                            (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + ActionBar.getCurrentActionBarHeight() + middleStateProfileExtraHeight
                    );
                }
            }
        };
        contentView = (SizeNotifierFrameLayout) fragmentView;
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        initListView(context);
        frameLayout.addView(listView);

        profileContainer = new ProfileContainerView(context);
        profileContainer.setBackgroundColor(getThemedColor(Theme.key_avatar_backgroundActionBarBlue));
        frameLayout.addView(profileContainer);
        initMenu();
        frameLayout.addView(actionBar);
        initAvatar(context);
        initNameTextView(context);
        initOnlineTextView(context);
        profileContainer.addView(avatarImage, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        initActions(context);
        profileContainer.addView(actionsContainer);
        giftsView = new ProfileGiftsViewV2(context, currentAccount, getDialogId(), profileContainer, avatarImage, resourcesProvider);
        profileContainer.addView(giftsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        updateProfileData();
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
                avatarImage.setImageDrawable(vectorAvatarThumbDrawable);
            } else if (videoThumbLocation != null && !user.photo.personal) {
                avatarImage.getImageReceiver().setVideoThumbIsSame(true);
                avatarImage.setImage(videoThumbLocation, "avatar", thumbLocation, "50_50", avatarDrawable, user);
            } else {
                avatarImage.setImage(videoLocation, ImageLoader.AUTOPLAY_FILTER, thumbLocation, "50_50", avatarDrawable, user);
            }
        } else if (chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(chatId);
            if (chat != null) {
                currentChat = chat;
            } else {
                chat = currentChat;
            }
            TLRPC.FileLocation photoBig = null;
            if (chat.photo != null && !isTopic) {
                photoBig = chat.photo.photo_big;
            }
            TLRPC.TL_forumTopic topic = null;

            if (isTopic) {
                topic = getMessagesController().getTopicsController().findTopic(chatId, topicId);
            }

            final ImageLocation imageLocation;
            final ImageLocation thumbLocation;
            final ImageLocation videoLocation;
            if (isTopic) {
                imageLocation = null;
                thumbLocation = null;
                videoLocation = null;
                ForumUtilities.setTopicIcon(avatarImage, topic, true, true, resourcesProvider);
            } else if (ChatObject.isMonoForum(currentChat)) {
                TLRPC.Chat channel = getMessagesController().getMonoForumLinkedChat(currentChat.id);
                avatarDrawable.setInfo(currentAccount, channel);
                imageLocation = ImageLocation.getForUserOrChat(channel, ImageLocation.TYPE_BIG);
                thumbLocation = ImageLocation.getForUserOrChat(channel, ImageLocation.TYPE_SMALL);
                //videoLocation = avatarsViewPager.getCurrentVideoLocation(thumbLocation, imageLocation);
                videoLocation = null;
            } else {
                avatarDrawable.setInfo(currentAccount, chat);
                imageLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_BIG);
                thumbLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_SMALL);
                //videoLocation = avatarsViewPager.getCurrentVideoLocation(thumbLocation, imageLocation);
                videoLocation = null;
            }

            //boolean initied = avatarsViewPager.initIfEmpty(null, imageLocation, thumbLocation, reload);
            //if ((imageLocation == null || initied) && isPulledDown) {
            //    final View view = layoutManager.findViewByPosition(0);
            //    if (view != null) {
            //        listView.smoothScrollBy(0, view.getTop() - AndroidUtilities.dp(88), CubicBezierInterpolator.EASE_OUT_QUINT);
            //    }
            //}
            String filter;
            if (videoLocation != null && videoLocation.imageType == FileLoader.IMAGE_TYPE_ANIMATION) {
                filter = ImageLoader.AUTOPLAY_FILTER;
            } else {
                filter = null;
            }
            //if (avatarBig == null && !isTopic) {
                avatarImage.setImage(videoLocation, filter, thumbLocation, "50_50", avatarDrawable, chat);
            //}
            if (imageLocation != null && (prevLoadedImageLocation == null || imageLocation.photoId != prevLoadedImageLocation.photoId)) {
                prevLoadedImageLocation = imageLocation;
                getFileLoader().loadFile(imageLocation, chat, null, FileLoader.PRIORITY_LOW, 1);
            }
            //avatarImage.getImageReceiver().setVisible(!PhotoViewer.isShowingImage(photoBig) && (getLastStoryViewer() == null || getLastStoryViewer().transitionViewHolder.view != avatarImage), storyView != null);
        }
    }

    private void updateProfileData() {
        String onlineTextOverride;
        int currentConnectionState = getConnectionsManager().getConnectionState();
        if (currentConnectionState == ConnectionsManager.ConnectionStateWaitingForNetwork) {
            onlineTextOverride = LocaleController.getString(R.string.WaitingForNetwork);
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateConnecting) {
            onlineTextOverride = LocaleController.getString(R.string.Connecting);
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateUpdating) {
            onlineTextOverride = LocaleController.getString(R.string.Updating);
        } else if (currentConnectionState == ConnectionsManager.ConnectionStateConnectingToProxy) {
            onlineTextOverride = LocaleController.getString(R.string.ConnectingToProxy);
        } else {
            onlineTextOverride = null;
        }
        BaseFragment prevFragment = null;
        if (parentLayout != null && parentLayout.getFragmentStack().size() >= 2) {
            BaseFragment fragment = parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2);
            if (fragment instanceof ChatActivityInterface) {
                prevFragment = fragment;
            }
            if (fragment instanceof DialogsActivity) {
                DialogsActivity dialogsActivity = (DialogsActivity) fragment;
                if (dialogsActivity.rightSlidingDialogContainer != null && dialogsActivity.rightSlidingDialogContainer.currentFragment instanceof ChatActivityInterface) {
                    previousTransitionMainFragment = dialogsActivity;
                    prevFragment = dialogsActivity.rightSlidingDialogContainer.currentFragment;
                }
            }
        }
        final boolean copyFromChatActivity = prevFragment instanceof ChatActivity && ((ChatActivity) prevFragment).avatarContainer != null && ((ChatActivity) prevFragment).getChatMode() == ChatActivity.MODE_SUGGESTIONS;

        TLRPC.TL_forumTopic topic = null;

        hasFallbackPhoto = false;
        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user == null) {
                return;
            }
            boolean shortStatus = user.photo != null && user.photo.personal;

            final MessagesController.PeerColor wasPeerColor = peerColor;
            peerColor = MessagesController.PeerColor.fromCollectible(user.emoji_status);
            if (peerColor == null) {
                final int colorId = UserObject.getProfileColorId(user);
                final MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).profilePeerColors;
                peerColor = peerColors == null ? null : peerColors.getColor(colorId);
            }
            if (wasPeerColor != peerColor) {
                updatedPeerColor();
            }

            CharSequence newString = UserObject.getUserName(user);
            String newString2;
            boolean hiddenStatusButton = false;
            if (user.id == getUserConfig().getClientUserId()) {
                if (UserObject.hasFallbackPhoto(userInfo)) {
                    newString2 = "";
                    hasFallbackPhoto = true;
                    TLRPC.PhotoSize smallSize = FileLoader.getClosestPhotoSizeWithSize(userInfo.fallback_photo.sizes, 1000);
                    if (smallSize != null) {
                        //fallbackImage.setImage(ImageLocation.getForPhoto(smallSize, userInfo.fallback_photo), "50_50", (Drawable) null, 0, null, UserConfig.getInstance(currentAccount).getCurrentUser(), 0);
                    }
                } else {
                    newString2 = LocaleController.getString(R.string.Online);
                }
            } else if (user.id == UserObject.VERIFY) {
                newString2 = LocaleController.getString(R.string.VerifyCodesNotifications);
            } else if (user.id == 333000 || user.id == 777000 || user.id == 42777) {
                newString2 = LocaleController.getString(R.string.ServiceNotifications);
            } else if (MessagesController.isSupportUser(user)) {
                newString2 = LocaleController.getString(R.string.SupportStatus);
            } else if (isBot) {
                if (user.bot_active_users != 0) {
                    newString2 = LocaleController.formatPluralStringComma("BotUsers", user.bot_active_users, ',');
                } else {
                    newString2 = LocaleController.getString(R.string.Bot);
                }

            } else {
                isOnline[0] = false;
                newString2 = LocaleController.formatUserStatus(currentAccount, user, isOnline, shortStatus ? new boolean[1] : null);
                hiddenStatusButton = user != null && !isOnline[0] && !getUserConfig().isPremium() && user.status != null && (user.status instanceof TLRPC.TL_userStatusRecently || user.status instanceof TLRPC.TL_userStatusLastMonth || user.status instanceof TLRPC.TL_userStatusLastWeek) && user.status.by_me;
                //if (onlineTextView[1] != null && !mediaHeaderVisible) {
                //    int key = isOnline[0] && peerColor == null ? Theme.key_profile_status : Theme.key_avatar_subtitleInProfileBlue;
                //    onlineTextView[1].setTag(key);
                //    if (!isPulledDown) {
                //        onlineTextView[1].setTextColor(applyPeerColor(getThemedColor(key), true, isOnline[0]));
                //    }
                //}
            }
            try {
                newString = Emoji.replaceEmoji(newString, nameTextView[1].getPaint().getFontMetricsInt(), false);
            } catch (Exception ignore) {
            }
            for (int a = 0; a < 2; a++) {
                if (nameTextView[a] == null) {
                    continue;
                }
                if (a == 0 && copyFromChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) prevFragment;
                    SimpleTextView titleTextView = chatActivity.avatarContainer.getTitleTextView();
                    nameTextView[a].setText(titleTextView.getText());
                    nameTextView[a].setRightDrawable(titleTextView.getRightDrawable());
                    nameTextView[a].setRightDrawable2(titleTextView.getRightDrawable2());
                } else if (a == 0 && user.id != getUserConfig().getClientUserId() && !MessagesController.isSupportUser(user) && user.phone != null && user.phone.length() != 0 && getContactsController().contactsDict.get(user.id) == null &&
                        (getContactsController().contactsDict.size() != 0 || !getContactsController().isLoadingContacts())) {
                    nameTextView[a].setText(PhoneFormat.getInstance().format("+" + user.phone));
                } else {
                    nameTextView[a].setText(newString);
                }
                if (a == 0 && onlineTextOverride != null) {
                    onlineTextView[a].setText(onlineTextOverride);
                } else if (a == 0 && copyFromChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) prevFragment;
                    if (chatActivity.avatarContainer.getSubtitleTextView() instanceof SimpleTextView) {
                        SimpleTextView textView = (SimpleTextView) chatActivity.avatarContainer.getSubtitleTextView();
                        onlineTextView[a].setText(textView.getText());
                    } else if (chatActivity.avatarContainer.getSubtitleTextView() instanceof AnimatedTextView) {
                        AnimatedTextView textView = (AnimatedTextView) chatActivity.avatarContainer.getSubtitleTextView();
                        onlineTextView[a].setText(textView.getText());
                    }
                } else {
                    onlineTextView[a].setText(newString2);
                }

                onlineTextView[a].setDrawablePadding(dp(9));
                onlineTextView[a].setRightDrawableInside(true);
                onlineTextView[a].setRightDrawable(a == 1 && hiddenStatusButton ? getShowStatusButton() : null);
                onlineTextView[a].setRightDrawableOnClick(a == 1 && hiddenStatusButton ? v -> {
                    MessagePrivateSeenView.showSheet(getContext(), currentAccount, getDialogId(), true, null, () -> {
                        getMessagesController().reloadUser(getDialogId());
                    }, resourcesProvider);
                } : null);
                Drawable leftIcon = currentEncryptedChat != null ? getLockIconDrawable() : null;
                boolean rightIconIsPremium = false, rightIconIsStatus = false;
                nameTextView[a].setRightDrawableOutside(a == 0);
                if (a == 0 && !copyFromChatActivity) {
                    if (user.scam || user.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(user.scam ? 0 : 1));
                        nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.ScamMessage);
                    } else if (user.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                        nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.AccDescrVerified);
                    } else if (getMessagesController().isDialogMuted(dialogId != 0 ? dialogId : userId, topicId)) {
                        nameTextView[a].setRightDrawable2(getThemedDrawable(Theme.key_drawable_muteIconDrawable));
                        nameTextViewRightDrawable2ContentDescription = LocaleController.getString(R.string.NotificationsMuted);
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                        nameTextViewRightDrawable2ContentDescription = null;
                    }
                    if (user != null && !getMessagesController().premiumFeaturesBlocked() && !MessagesController.isSupportUser(user) && DialogObject.getEmojiStatusDocumentId(user.emoji_status) != 0) {
                        rightIconIsStatus = true;
                        rightIconIsPremium = false;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(user.emoji_status, false, a));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.AccDescrPremium);
                    } else if (getMessagesController().isPremiumUser(user)) {
                        rightIconIsStatus = false;
                        rightIconIsPremium = true;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(null, false, a));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString( R.string.AccDescrPremium);
                    } else {
                        nameTextView[a].setRightDrawable(null);
                        nameTextViewRightDrawableContentDescription = null;
                    }
                } else if (a == 1) {
                    if (user.scam || user.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(user.scam ? 0 : 1));
                    } else if (user.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                    }
                    if (!getMessagesController().premiumFeaturesBlocked() && user != null && !MessagesController.isSupportUser(user) && DialogObject.getEmojiStatusDocumentId(user.emoji_status) != 0) {
                        rightIconIsStatus = true;
                        rightIconIsPremium = false;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(user.emoji_status, true, a));
                    } else if (getMessagesController().isPremiumUser(user)) {
                        rightIconIsStatus = false;
                        rightIconIsPremium = true;
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(null, true, a));
                    } else {
                        nameTextView[a].setRightDrawable(null);
                    }
                }
                if (leftIcon == null && currentEncryptedChat == null && user.bot_verification_icon != 0) {
                    nameTextView[a].setLeftDrawableOutside(true);
                    leftIcon = getBotVerificationDrawable(user.bot_verification_icon, false, a);
                } else {
                    nameTextView[a].setLeftDrawableOutside(false);
                }
                nameTextView[a].setLeftDrawable(leftIcon);
                if (a == 1 && (rightIconIsStatus || rightIconIsPremium)) {
                    nameTextView[a].setRightDrawableOutside(true);
                }
                if (user.self && getMessagesController().isPremiumUser(user)) {
                    //nameTextView[a].setRightDrawableOnClick(v -> {
                    //    showStatusSelect();
                    //});
                }
                if (!user.self && getMessagesController().isPremiumUser(user)) {
                    final SimpleTextView textView = nameTextView[a];
                    nameTextView[a].setRightDrawableOnClick(v -> {
                        if (user.emoji_status instanceof TLRPC.TL_emojiStatusCollectible) {
                            TLRPC.TL_emojiStatusCollectible status = (TLRPC.TL_emojiStatusCollectible) user.emoji_status;
                            if (status != null) {
                                Browser.openUrl(getContext(), "https://" + getMessagesController().linkPrefix + "/nft/" + status.slug);
                            }
                            return;
                        }
                        PremiumPreviewBottomSheet premiumPreviewBottomSheet = new PremiumPreviewBottomSheet(ProfileActivityV2.this, currentAccount, user, resourcesProvider);
                        int[] coords = new int[2];
                        textView.getLocationOnScreen(coords);
                        premiumPreviewBottomSheet.startEnterFromX = textView.rightDrawableX;
                        premiumPreviewBottomSheet.startEnterFromY = textView.rightDrawableY;
                        premiumPreviewBottomSheet.startEnterFromScale = textView.getScaleX();
                        premiumPreviewBottomSheet.startEnterFromX1 = textView.getLeft();
                        premiumPreviewBottomSheet.startEnterFromY1 = textView.getTop();
                        premiumPreviewBottomSheet.startEnterFromView = textView;
                        if (textView.getRightDrawable() == emojiStatusDrawable[1] && emojiStatusDrawable[1] != null && emojiStatusDrawable[1].getDrawable() instanceof AnimatedEmojiDrawable) {
                            premiumPreviewBottomSheet.startEnterFromScale *= 0.98f;
                            TLRPC.Document document = ((AnimatedEmojiDrawable) emojiStatusDrawable[1].getDrawable()).getDocument();
                            if (document != null) {
                                BackupImageView icon = new BackupImageView(getContext());
                                String filter = "160_160";
                                ImageLocation mediaLocation;
                                String mediaFilter;
                                SvgHelper.SvgDrawable thumbDrawable = DocumentObject.getSvgThumb(document.thumbs, Theme.key_windowBackgroundWhiteGrayIcon, 0.2f);
                                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
                                if ("video/webm".equals(document.mime_type)) {
                                    mediaLocation = ImageLocation.getForDocument(document);
                                    mediaFilter = filter + "_" + ImageLoader.AUTOPLAY_FILTER;
                                    if (thumbDrawable != null) {
                                        thumbDrawable.overrideWidthAndHeight(512, 512);
                                    }
                                } else {
                                    if (thumbDrawable != null && MessageObject.isAnimatedStickerDocument(document, false)) {
                                        thumbDrawable.overrideWidthAndHeight(512, 512);
                                    }
                                    mediaLocation = ImageLocation.getForDocument(document);
                                    mediaFilter = filter;
                                }
                                icon.setLayerNum(7);
                                icon.setRoundRadius(AndroidUtilities.dp(4));
                                icon.setImage(mediaLocation, mediaFilter, ImageLocation.getForDocument(thumb, document), "140_140", thumbDrawable, document);
                                if (((AnimatedEmojiDrawable) emojiStatusDrawable[1].getDrawable()).canOverrideColor()) {
                                    icon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteBlueIcon), PorterDuff.Mode.SRC_IN));
                                    premiumPreviewBottomSheet.statusStickerSet = MessageObject.getInputStickerSet(document);
                                } else {
                                    premiumPreviewBottomSheet.statusStickerSet = MessageObject.getInputStickerSet(document);
                                }
                                premiumPreviewBottomSheet.overrideTitleIcon = icon;
                                premiumPreviewBottomSheet.isEmojiStatus = true;
                            }
                        }
                        showDialog(premiumPreviewBottomSheet);
                    });
                }
            }
            if (userId == UserConfig.getInstance(currentAccount).clientUserId) {
                onlineTextView[2].setText(LocaleController.getString(R.string.FallbackTooltip));
                onlineTextView[3].setText(LocaleController.getString(R.string.Online));
            } else {
                if (user.photo != null && user.photo.personal && user.photo.has_video) {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(newString2);
                    spannableStringBuilder.setSpan(new EmptyStubSpan(), 0, newString2.length(), 0);
                    spannableStringBuilder.append(" d ");
                    spannableStringBuilder.append(LocaleController.getString(R.string.CustomAvatarTooltipVideo));
                    spannableStringBuilder.setSpan(new DotDividerSpan(), newString2.length() + 1, newString2.length() + 2, 0);
                    onlineTextView[2].setText(spannableStringBuilder);
                } else {
                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(newString2);
                    spannableStringBuilder.setSpan(new EmptyStubSpan(), 0, newString2.length(), 0);
                    spannableStringBuilder.append(" d ");
                    spannableStringBuilder.append(LocaleController.getString(R.string.CustomAvatarTooltip));
                    spannableStringBuilder.setSpan(new DotDividerSpan(), newString2.length() + 1, newString2.length() + 2, 0);
                    onlineTextView[2].setText(spannableStringBuilder);
                }
            }

            //onlineTextView[2].setVisibility(View.VISIBLE);
            //if (!searchMode) {
            //    onlineTextView[3].setVisibility(View.VISIBLE);
            //}
            //
            //if (previousTransitionFragment != null) {
            //    previousTransitionFragment.checkAndUpdateAvatar();
            //}
            //avatarImage.getImageReceiver().setVisible(!PhotoViewer.isShowingImage(photoBig) && (getLastStoryViewer() == null || getLastStoryViewer().transitionViewHolder.view != avatarImage), storyView != null);
        } else if (chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(chatId);
            if (chat != null) {
                currentChat = chat;
            } else {
                chat = currentChat;
            }

            final MessagesController.PeerColor wasPeerColor = peerColor;
            peerColor = MessagesController.PeerColor.fromCollectible(chat.emoji_status);
            if (peerColor == null) {
                final int colorId = ChatObject.getProfileColorId(chat);
                MessagesController.PeerColors peerColors = MessagesController.getInstance(currentAccount).profilePeerColors;
                peerColor = peerColors == null ? null : peerColors.getColor(colorId);
            }
            if (wasPeerColor != peerColor) {
                updatedPeerColor();
            }

            if (isTopic) {
                topic = getMessagesController().getTopicsController().findTopic(chatId, topicId);
            }

            CharSequence statusString;
            CharSequence profileStatusString;
            boolean profileStatusIsButton = false;
            if (ChatObject.isChannel(chat)) {
                if (!isTopic && (chatInfo == null || !currentChat.megagroup && (chatInfo.participants_count == 0 || ChatObject.hasAdminRights(currentChat) || chatInfo.can_view_participants))) {
                    if (currentChat.megagroup) {
                        statusString = profileStatusString = LocaleController.getString(R.string.Loading).toLowerCase();
                    } else {
                        if (ChatObject.isPublic(chat)) {
                            statusString = profileStatusString = LocaleController.getString(R.string.ChannelPublic).toLowerCase();
                        } else {
                            statusString = profileStatusString = LocaleController.getString(R.string.ChannelPrivate).toLowerCase();
                        }
                    }
                } else {
                    if (isTopic) {
                        int count = 0;
                        if (topic != null) {
                            count = topic.totalMessagesCount - 1;
                        }
                        if (count > 0) {
                            statusString = LocaleController.formatPluralString("messages", count, count);
                        } else {
                            statusString = formatString("TopicProfileStatus", R.string.TopicProfileStatus, chat.title);
                        }
                        SpannableString arrowString = new SpannableString(">");
                        arrowString.setSpan(new ColoredImageSpan(R.drawable.arrow_newchat), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        profileStatusString = new SpannableStringBuilder(chat.title).append(' ').append(arrowString);
                        profileStatusIsButton = true;
                    } else if (currentChat.megagroup) {
                        if (onlineCount > 1 && chatInfo.participants_count != 0) {
                            statusString = String.format("%s, %s", LocaleController.formatPluralString("Members", chatInfo.participants_count), LocaleController.formatPluralString("OnlineCount", Math.min(onlineCount, chatInfo.participants_count)));
                            profileStatusString = String.format("%s, %s", LocaleController.formatPluralStringComma("Members", chatInfo.participants_count), LocaleController.formatPluralStringComma("OnlineCount", Math.min(onlineCount, chatInfo.participants_count)));
                        } else {
                            if (chatInfo.participants_count == 0) {
                                if (chat.has_geo) {
                                    statusString = profileStatusString = LocaleController.getString(R.string.MegaLocation).toLowerCase();
                                } else if (ChatObject.isPublic(chat)) {
                                    statusString = profileStatusString = LocaleController.getString(R.string.MegaPublic).toLowerCase();
                                } else {
                                    statusString = profileStatusString = LocaleController.getString(R.string.MegaPrivate).toLowerCase();
                                }
                            } else {
                                statusString = LocaleController.formatPluralString("Members", chatInfo.participants_count);
                                profileStatusString = LocaleController.formatPluralStringComma("Members", chatInfo.participants_count);
                            }
                        }
                    } else {
                        if (currentChat.megagroup) {
                            statusString = LocaleController.formatPluralString("Members", chatInfo.participants_count);
                            profileStatusString = LocaleController.formatPluralStringComma("Members", chatInfo.participants_count);
                        } else {
                            statusString = LocaleController.formatPluralString("Subscribers", chatInfo.participants_count);
                            profileStatusString = LocaleController.formatPluralStringComma("Subscribers", chatInfo.participants_count);
                        }
                    }
                }
            } else {
                if (ChatObject.isKickedFromChat(chat)) {
                    statusString = profileStatusString = LocaleController.getString(R.string.YouWereKicked);
                } else if (ChatObject.isLeftFromChat(chat)) {
                    statusString = profileStatusString = LocaleController.getString(R.string.YouLeft);
                } else {
                    int count = chat.participants_count;
                    if (chatInfo != null && chatInfo.participants != null) {
                        count = chatInfo.participants.participants.size();
                    }
                    if (count != 0 && onlineCount > 1) {
                        statusString = profileStatusString = String.format("%s, %s", LocaleController.formatPluralString("Members", count), LocaleController.formatPluralString("OnlineCount", onlineCount));
                    } else {
                        statusString = profileStatusString = LocaleController.formatPluralString("Members", count);
                    }
                }
            }
            if (copyFromChatActivity) {
                ChatActivity chatActivity = (ChatActivity) prevFragment;
                if (chatActivity.avatarContainer.getSubtitleTextView() instanceof SimpleTextView) {
                    statusString = ((SimpleTextView) chatActivity.avatarContainer.getSubtitleTextView()).getText();
                } else if (chatActivity.avatarContainer.getSubtitleTextView() instanceof AnimatedTextView) {
                    statusString = ((AnimatedTextView) chatActivity.avatarContainer.getSubtitleTextView()).getText();
                }
            }
            for (int a = 0; a < 2; a++) {
                if (nameTextView[a] == null) {
                    continue;
                }
                if (a == 0 && copyFromChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) prevFragment;
                    SimpleTextView titleTextView = chatActivity.avatarContainer.getTitleTextView();
                    nameTextView[a].setText(titleTextView.getText());
                    nameTextView[a].setRightDrawable(titleTextView.getRightDrawable());
                    nameTextView[a].setRightDrawable2(titleTextView.getRightDrawable2());
                } else if (isTopic) {
                    CharSequence title = topic == null ? "" : topic.title;
                    try {
                        title = Emoji.replaceEmoji(title, nameTextView[a].getPaint().getFontMetricsInt(), false);
                    } catch (Exception ignore) {
                    }
                    nameTextView[a].setText(title);
                } else if (ChatObject.isMonoForum(chat)) {
                    CharSequence title = getString(R.string.ChatMessageSuggestions);
                    nameTextView[a].setText(title);
                } else if (chat.title != null) {
                    CharSequence title = chat.title;
                    try {
                        title = Emoji.replaceEmoji(title, nameTextView[a].getPaint().getFontMetricsInt(), false);
                    } catch (Exception ignore) {
                    }
                    nameTextView[a].setText(title);
                }
                nameTextView[a].setLeftDrawableOutside(false);
                nameTextView[a].setLeftDrawable(null);
                nameTextView[a].setRightDrawableOutside(a == 0);
                nameTextView[a].setRightDrawableOnClick(null);
                if (a != 0) {
                    if (chat.scam || chat.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(chat.scam ? 0 : 1));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.ScamMessage);
                    } else if (chat.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a));
                        nameTextViewRightDrawableContentDescription = LocaleController.getString(R.string.AccDescrVerified);
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                        nameTextViewRightDrawableContentDescription = null;
                    }
                    if (DialogObject.getEmojiStatusDocumentId(chat.emoji_status) != 0) {
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(chat.emoji_status, false, a));
                        nameTextView[a].setRightDrawableOutside(true);
                        nameTextViewRightDrawableContentDescription = null;
                        if (ChatObject.canChangeChatInfo(chat)) {
                            //nameTextView[a].setRightDrawableOnClick(v -> {
                            //    showStatusSelect();
                            //});
                            getMediaDataController().loadRestrictedStatusEmojis();
                        } else if (chat.emoji_status instanceof TLRPC.TL_emojiStatusCollectible) {
                            final String slug = ((TLRPC.TL_emojiStatusCollectible) chat.emoji_status).slug;
                            nameTextView[a].setRightDrawableOnClick(v -> {
                                Browser.openUrl(getContext(), "https://" + getMessagesController().linkPrefix + "/nft/" + slug);
                            });
                        }
                    }
                } else if (!copyFromChatActivity) {
                    if (chat.scam || chat.fake) {
                        nameTextView[a].setRightDrawable2(getScamDrawable(chat.scam ? 0 : 1));
                    } else if (chat.verified) {
                        nameTextView[a].setRightDrawable2(getVerifiedCrossfadeDrawable(a)) ;
                    } else if (getMessagesController().isDialogMuted(-chatId, topicId)) {
                        nameTextView[a].setRightDrawable2(getThemedDrawable(Theme.key_drawable_muteIconDrawable));
                    } else {
                        nameTextView[a].setRightDrawable2(null);
                    }
                    if (DialogObject.getEmojiStatusDocumentId(chat.emoji_status) != 0) {
                        nameTextView[a].setRightDrawable(getEmojiStatusDrawable(chat.emoji_status, false, a));
                        nameTextView[a].setRightDrawableOutside(true);
                    } else {
                        nameTextView[a].setRightDrawable(null);
                    }
                }
                if (chat.bot_verification_icon != 0) {
                    nameTextView[a].setLeftDrawableOutside(true);
                    nameTextView[a].setLeftDrawable(getBotVerificationDrawable(chat.bot_verification_icon, false, a));
                } else {
                    nameTextView[a].setLeftDrawable(null);
                }
                if (a == 0 && onlineTextOverride != null) {
                    onlineTextView[a].setText(onlineTextOverride);
                } else {
                    if (copyFromChatActivity || (currentChat.megagroup && chatInfo != null && onlineCount > 0) || isTopic) {
                        onlineTextView[a].setText(a == 0 ? statusString : profileStatusString);
                    } else if (a == 0 && ChatObject.isChannel(currentChat) && chatInfo != null && chatInfo.participants_count != 0 && (currentChat.megagroup || currentChat.broadcast)) {
                        int[] result = new int[1];
                        boolean ignoreShort = AndroidUtilities.isAccessibilityScreenReaderEnabled();
                        String shortNumber = ignoreShort ? String.valueOf(result[0] = chatInfo.participants_count) : LocaleController.formatShortNumber(chatInfo.participants_count, result);
                        if (currentChat.megagroup) {
                            if (chatInfo.participants_count == 0) {
                                if (chat.has_geo) {
                                    onlineTextView[a].setText(LocaleController.getString(R.string.MegaLocation).toLowerCase());
                                } else if (ChatObject.isPublic(chat)) {
                                    onlineTextView[a].setText(LocaleController.getString(R.string.MegaPublic).toLowerCase());
                                } else {
                                    onlineTextView[a].setText(LocaleController.getString(R.string.MegaPrivate).toLowerCase());
                                }
                            } else {
                                onlineTextView[a].setText(LocaleController.formatPluralString("Members", result[0]).replace(String.format("%d", result[0]), shortNumber));
                            }
                        } else {
                            onlineTextView[a].setText(LocaleController.formatPluralString("Subscribers", result[0]).replace(String.format("%d", result[0]), shortNumber));
                        }
                    } else {
                        onlineTextView[a].setText(a == 0 ? statusString : profileStatusString);
                    }
                }
                if (a == 1 && isTopic) {
                    if (profileStatusIsButton) {
                        //onlineTextView[a].setOnClickListener(e -> goToForum());
                    } else {
                        onlineTextView[a].setOnClickListener(null);
                        onlineTextView[a].setClickable(false);
                    }
                }
            }
        }
    }

    private void updatedPeerColor() {
        adaptedColors.clear();
        if (profileContainer != null) {
            profileContainer.setBackgroundColorId(peerColor, true);
        }
        if (onlineTextView[1] != null) {
            int statusColor;
            if (onlineTextView[1].getTag() instanceof Integer) {
                statusColor = getThemedColor((Integer) onlineTextView[1].getTag());
            } else {
                statusColor = getThemedColor(Theme.key_avatar_subtitleInProfileBlue);
            }
            //onlineTextView[1].setTextColor(ColorUtils.blendARGB(applyPeerColor(statusColor, true, isOnline[0]), 0xB3FFFFFF, currentExpandAnimatorValue));
        }
        if (showStatusButton != null) {
            //showStatusButton.setBackgroundColor(ColorUtils.blendARGB(Theme.multAlpha(Theme.adaptHSV(actionBarBackgroundColor, +0.18f, -0.1f), 0.5f), 0x23ffffff, currentExpandAnimatorValue));
        }
        if (actionBar != null) {
            //actionBar.setItemsColor(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_actionBarDefaultIcon), getThemedColor(Theme.key_actionBarActionModeDefaultIcon), mediaHeaderAnimationProgress), false);
            //actionBar.setItemsBackgroundColor(ColorUtils.blendARGB(peerColor != null ? Theme.ACTION_BAR_WHITE_SELECTOR_COLOR : peerColor != null ? 0x20ffffff : getThemedColor(Theme.key_avatar_actionBarSelectorBlue), getThemedColor(Theme.key_actionBarActionModeDefaultSelector), mediaHeaderAnimationProgress), false);
        }
        //if (verifiedDrawable[1] != null) {
        //    final int color1 = peerColor != null ? Theme.adaptHSV(ColorUtils.blendARGB(peerColor.getColor2(), peerColor.hasColor6(Theme.isCurrentThemeDark()) ? peerColor.getColor5() : peerColor.getColor3(), .4f), +.1f, Theme.isCurrentThemeDark() ? -.1f : -.08f) : getThemedColor(Theme.key_profile_verifiedBackground);
        //    final int color2 = getThemedColor(Theme.key_player_actionBarTitle);
        //    verifiedDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
        //}
        //if (verifiedCheckDrawable[1] != null) {
        //    final int color1 = peerColor != null ? Color.WHITE : dontApplyPeerColor(getThemedColor(Theme.key_profile_verifiedCheck));
        //    final int color2 = getThemedColor(Theme.key_windowBackgroundWhite);
        //    verifiedCheckDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color1, color2, mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
        //}
        if (nameTextView[1] != null) {
            //nameTextView[1].setTextColor(ColorUtils.blendARGB(ColorUtils.blendARGB(peerColor != null ? Color.WHITE : getThemedColor(Theme.key_profile_title), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress), Color.WHITE, currentExpandAnimatorValue));
        }
        //if (autoDeletePopupWrapper != null && autoDeletePopupWrapper.textView != null) {
        //    autoDeletePopupWrapper.textView.invalidate();
        //}
        AndroidUtilities.forEachViews(listView, view -> {
            if (view instanceof HeaderCell) {
                ((HeaderCell) view).setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
            } else if (view instanceof TextDetailCell) {
                ((TextDetailCell) view).updateColors();
            } else if (view instanceof TextCell) {
                ((TextCell) view).updateColors();
            } else if (view instanceof AboutLinkCell) {
                ((AboutLinkCell) view).updateColors();
            } else if (view instanceof NotificationsCheckCell) {
                ((NotificationsCheckCell) view).getCheckBox().invalidate();
            } else if (view instanceof ProfileHoursCell) {
                ((ProfileHoursCell) view).updateColors();
            } else if (view instanceof ProfileChannelCell) {
                ((ProfileChannelCell) view).updateColors();
            }
        });
        //if (sharedMediaLayout != null && sharedMediaLayout.scrollSlidingTextTabStrip != null) {
        //    sharedMediaLayout.scrollSlidingTextTabStrip.updateColors();
        //}
        //if (sharedMediaLayout != null && sharedMediaLayout.giftsContainer != null) {
        //    sharedMediaLayout.giftsContainer.updateColors();
        //}
        updateEmojiStatusDrawableColor();
        //if (storyView != null) {
        //    storyView.update();
        //}
        if (giftsView != null) {
            giftsView.update();
        }
    }

    private Drawable getLockIconDrawable() {
        if (lockIconDrawable == null) {
            lockIconDrawable = Theme.chat_lockIconDrawable.getConstantState().newDrawable().mutate();
        }
        return lockIconDrawable;
    }

    private ProfileActivity.ShowDrawable showStatusButton;
    public ProfileActivity.ShowDrawable getShowStatusButton() {
        if (showStatusButton == null) {
            showStatusButton = new ProfileActivity.ShowDrawable(LocaleController.getString(R.string.StatusHiddenShow));
            //showStatusButton.setAlpha((int) (0xFF * Math.min(1f, extraHeight / AndroidUtilities.dp(88f))));
            //showStatusButton.setBackgroundColor(ColorUtils.blendARGB(Theme.multAlpha(Theme.adaptHSV(actionBarBackgroundColor, +0.18f, -0.1f), 0.5f), 0x23ffffff, currentExpandAnimatorValue));
        }
        return showStatusButton;
    }

    private Drawable getEmojiStatusDrawable(TLRPC.EmojiStatus emojiStatus, boolean animated, int a) {
        if (emojiStatusDrawable[a] == null) {
            emojiStatusDrawable[a] = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(nameTextView[a], AndroidUtilities.dp(24), a == 0 ? AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS : AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD);
            if (fragmentViewAttached) {
                emojiStatusDrawable[a].attach();
            }
        }
        if (a == 1) {
            emojiStatusGiftId = null;
        }
        if (emojiStatus instanceof TLRPC.TL_emojiStatus) {
            final TLRPC.TL_emojiStatus status = (TLRPC.TL_emojiStatus) emojiStatus;
            if ((status.flags & 1) == 0 || status.until > (int) (System.currentTimeMillis() / 1000)) {
                emojiStatusDrawable[a].set(status.document_id, animated);
                emojiStatusDrawable[a].setParticles(false, animated);
            } else {
                emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), animated);
                emojiStatusDrawable[a].setParticles(false, animated);
            }
        } else if (emojiStatus instanceof TLRPC.TL_emojiStatusCollectible) {
            final TLRPC.TL_emojiStatusCollectible status = (TLRPC.TL_emojiStatusCollectible) emojiStatus;
            if ((status.flags & 1) == 0 || status.until > (int) (System.currentTimeMillis() / 1000)) {
                if (a == 1) {
                    emojiStatusGiftId = status.collectible_id;
                }
                emojiStatusDrawable[a].set(status.document_id, animated);
                emojiStatusDrawable[a].setParticles(true, animated);
            } else {
                emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), animated);
                emojiStatusDrawable[a].setParticles(false, animated);
            }
        } else {
            emojiStatusDrawable[a].set(getPremiumCrossfadeDrawable(a), animated);
            emojiStatusDrawable[a].setParticles(false, animated);
        }
        updateEmojiStatusDrawableColor();
        return emojiStatusDrawable[a];
    }

    private Drawable getPremiumCrossfadeDrawable(int a) {
        if (premiumCrossfadeDrawable[a] == null) {
            premiumStarDrawable[a] = ContextCompat.getDrawable(getParentActivity(), R.drawable.msg_premium_liststar).mutate();
            int color = getThemedColor(Theme.key_profile_verifiedBackground);
            premiumStarDrawable[a].setColorFilter(color, PorterDuff.Mode.MULTIPLY);
            premiumCrossfadeDrawable[a] = new CrossfadeDrawable(premiumStarDrawable[a], ContextCompat.getDrawable(getParentActivity(), R.drawable.msg_premium_prolfilestar).mutate());
        }
        return premiumCrossfadeDrawable[a];
    }

    private float lastEmojiStatusProgress;
    private void updateEmojiStatusDrawableColor() {
        updateEmojiStatusDrawableColor(lastEmojiStatusProgress);
    }

    private void updateEmojiStatusDrawableColor(float progress) {
        for (int a = 0; a < 2; ++a) {
            final int fromColor;
            //if (peerColor != null && a == 1) {
            //    fromColor = ColorUtils.blendARGB(peerColor.getStoryColor1(Theme.isCurrentThemeDark()), 0xFFFFFFFF, 0.25f);
            //} else {
            //    fromColor = AndroidUtilities.getOffsetColor(getThemedColor(Theme.key_profile_verifiedBackground), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress, 1.0f);
            //}
            //final int color = ColorUtils.blendARGB(ColorUtils.blendARGB(fromColor, 0xffffffff, progress), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress);
            //if (emojiStatusDrawable[a] != null) {
            //    emojiStatusDrawable[a].setColor(color);
            //}
            //if (botVerificationDrawable[a] != null) {
            //    botVerificationDrawable[a].setColor(ColorUtils.blendARGB(ColorUtils.blendARGB(fromColor, 0x99ffffff, progress), getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress));
            //}
            //if (a == 1) {
            //    animatedStatusView.setColor(color);
            //}
        }
        lastEmojiStatusProgress = progress;
    }

    private Drawable getBotVerificationDrawable(long icon, boolean animated, int a) {
        //if (botVerificationDrawable[a] == null) {
        //    botVerificationDrawable[a] = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(nameTextView[a], AndroidUtilities.dp(17), a == 0 ? AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS : AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD);
        //    botVerificationDrawable[a].offset(0, dp(1));
        //    if (fragmentViewAttached) {
        //        botVerificationDrawable[a].attach();
        //    }
        //}
        //if (icon != 0) {
        //    botVerificationDrawable[a].set(icon, animated);
        //} else {
        //    botVerificationDrawable[a].set((Drawable) null, animated);
        //}
        //updateEmojiStatusDrawableColor();
        //return botVerificationDrawable[a];
        return null;
    }

    private ScamDrawable scamDrawable;
    private Drawable getScamDrawable(int type) {
        if (scamDrawable == null) {
            scamDrawable = new ScamDrawable(11, type);
            scamDrawable.setColor(getThemedColor(Theme.key_avatar_subtitleInProfileBlue));
        }
        return scamDrawable;
    }

    private final Drawable[] verifiedCheckDrawable = new Drawable[2];
    private final CrossfadeDrawable[] verifiedCrossfadeDrawable = new CrossfadeDrawable[2];
    private final Drawable[] verifiedDrawable = new Drawable[2];

    private Drawable getVerifiedCrossfadeDrawable(int a) {
        if (verifiedCrossfadeDrawable[a] == null) {
            verifiedDrawable[a] = Theme.profile_verifiedDrawable.getConstantState().newDrawable().mutate();
            verifiedCheckDrawable[a] = Theme.profile_verifiedCheckDrawable.getConstantState().newDrawable().mutate();
            //if (a == 1 && peerColor != null) {
            //    int color = Theme.adaptHSV(peerColor.hasColor6(Theme.isCurrentThemeDark()) ? peerColor.getColor5() : peerColor.getColor3(), +.1f, Theme.isCurrentThemeDark() ? -.1f : -.08f);
            //    verifiedDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color, getThemedColor(Theme.key_player_actionBarTitle), mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
            //    color = Color.WHITE;
            //    verifiedCheckDrawable[1].setColorFilter(AndroidUtilities.getOffsetColor(color, getThemedColor(Theme.key_windowBackgroundWhite), mediaHeaderAnimationProgress, 1.0f), PorterDuff.Mode.MULTIPLY);
            //}
            verifiedCrossfadeDrawable[a] = new CrossfadeDrawable(
                    new CombinedDrawable(verifiedDrawable[a], verifiedCheckDrawable[a]),
                    ContextCompat.getDrawable(getParentActivity(), R.drawable.verified_profile)
            );
        }
        return verifiedCrossfadeDrawable[a];
    }

    public long getDialogId() {
        if (dialogId != 0) {
            return dialogId;
        } else if (userId != 0) {
            return userId;
        } else {
            return -chatId;
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

    private void initMenu() {
        ActionBarMenu menu = actionBar.createMenu();
        otherItem = menu.addItem(10, R.drawable.ic_ab_other, resourcesProvider);
        otherItem.setContentDescription(LocaleController.getString(R.string.AccDescrMoreOptions));
        otherItem.setIconColor(getThemedColor(Theme.key_actionBarDefaultIcon));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(final int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
    }

    private void initActions(Context context) {
        actionsContainer = new ActionsContainer(context);

        messageActionVisible = false;
        callActionVisible = false;
        videoCallActionVisible = false;
        notificationsActionVisible = false;
        giftActionVisible = false;
        shareActionVisible = false;
        joinActionVisible = false;
        leaveActionVisible = false;
        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user == null) {
                return;
            }
            if (!UserObject.isUserSelf(user)) {
                if (userInfo != null && userInfo.phone_calls_available) {
                    callActionVisible = true;
                    videoCallActionVisible = Build.VERSION.SDK_INT >= 18 && userInfo.video_calls_available;
                }
                if (userId != getUserConfig().getClientUserId()) {
                    notificationsActionVisible = true;
                }
                messageActionVisible = true;

                if (!isBot && getContactsController().contactsDict.get(userId) != null) {
                    if (!UserObject.isDeleted(user) && !isBot && currentEncryptedChat == null && !userBlocked && userId != 333000 && userId != 777000 && userId != 42777) {
                        if (!BuildVars.IS_BILLING_UNAVAILABLE && !user.self && !user.bot && !MessagesController.isSupportUser(user) && !getMessagesController().premiumPurchaseBlocked()) {
                            StarsController.getInstance(currentAccount).loadStarGifts();
                            giftActionVisible = !videoCallActionVisible;
                        }
                    }
                }
                if (isBot && !MessagesController.isSupportUser(user) && getDialogId() != UserObject.VERIFY) {
                    shareActionVisible = true;
                }
            }
        } else if (chatId != 0) {
            TLRPC.Chat chat = getMessagesController().getChat(chatId);
            if (ChatObject.isChannel(chat)) {
                if (chatInfo != null) {
                    ChatObject.Call call = getMessagesController().getGroupCall(chatId, false);
                    callActionVisible = call != null;
                }
                if (currentChat.left && !currentChat.kicked) {
                    joinActionVisible = true;
                }
                if (chat.megagroup) {
                    if (!chat.creator && !chat.left && !chat.kicked && !isTopic) {
                        leaveActionVisible = true;
                    }
                } else {
                    if (!currentChat.creator && !currentChat.left && !currentChat.kicked) {
                        leaveActionVisible = true;
                    }
                }
            } else {
                if (chatInfo != null) {
                    ChatObject.Call call = getMessagesController().getGroupCall(chatId, false);
                    callActionVisible = call != null;
                }
                leaveActionVisible = true;
            }
            notificationsActionVisible = true;
            if (ChatObject.isPublic(chat) && !chat.megagroup) {
                shareActionVisible = true;
            }
        }
        if (joinActionVisible) {
            // Should be localised
            actionsContainer.addAction(R.drawable.join, "Join", v -> {});
        }
        if (messageActionVisible) {
            actionsContainer.addAction(R.drawable.message, LocaleController.getString(R.string.Message), v -> onMessageClick());
        }
        if (notificationsActionVisible) {
            if (getMessagesController().isDialogMuted(getDialogId(), topicId)) {
                actionsContainer.addAction(R.drawable.unmute, LocaleController.getString(R.string.Unmute), v -> {});
            } else {
                actionsContainer.addAction(R.drawable.mute, LocaleController.getString(R.string.Mute), v -> {});
            }
        }
        if (callActionVisible) {
            actionsContainer.addAction(R.drawable.call, LocaleController.getString(R.string.Call), v -> onCallClick(false));
        }
        if (videoCallActionVisible) {
            // Should be localised
            actionsContainer.addAction(R.drawable.video, "Video", v -> onCallClick(true));
        }

        if (giftActionVisible) {
            // Should be localised
            actionsContainer.addAction(R.drawable.gift, "Gift", v -> {});
        }
        if (shareActionVisible) {
            actionsContainer.addAction(R.drawable.share, LocaleController.getString(R.string.BotShare), v -> {});
        }
        if (leaveActionVisible) {
            // Should be localised
            actionsContainer.addAction(R.drawable.leave, "Leave", v -> onLeaveClick());
        }
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
        chatId = arguments.getLong("chat_id", 0);
        topicId = arguments.getLong("topic_id", 0);
        isTopic = topicId != 0;

        if (userId != 0) {
            dialogId = arguments.getLong("dialog_id", 0);
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user == null) {
                return false;
            }
            userInfo = getMessagesController().getUserFull(userId);
            userBlocked = getMessagesController().blockePeers.indexOfKey(userId) >= 0;
            if (user.bot) {
                isBot = true;
                getMediaDataController().loadBotInfo(user.id, user.id, true, classGuid);
            }
        } else if (chatId != 0) {
            currentChat = getMessagesController().getChat(chatId);
            if (currentChat == null) {
                final CountDownLatch countDownLatch = new CountDownLatch(1);
                getMessagesStorage().getStorageQueue().postRunnable(() -> {
                    currentChat = getMessagesStorage().getChat(chatId);
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (currentChat != null) {
                    getMessagesController().putChat(currentChat, true);
                } else {
                    return false;
                }
            }
            if (chatInfo == null) {
                chatInfo = getMessagesController().getChatFull(chatId);
            }
            if (ChatObject.isChannel(currentChat)) {
                getMessagesController().loadFullChat(chatId, classGuid, true);
            } else if (chatInfo == null) {
                chatInfo = getMessagesStorage().loadChatInfo(chatId, false, null, false, false);
            }
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (avatarImage != null) {
            avatarImage.setImageDrawable(null);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

    }

    private class ProfileContainerView extends FrameLayout {

        private boolean hasColorById;
        public int color1, color2;
        private int emojiColor;
        private int currentColor;
        private final AnimatedFloat hasColorAnimated = new AnimatedFloat(this, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
        private final AnimatedColor color1Animated = new AnimatedColor(this, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
        private final AnimatedColor color2Animated = new AnimatedColor(this, 350, CubicBezierInterpolator.EASE_OUT_QUINT);
        private int backgroundGradientColor1, backgroundGradientColor2, backgroundGradientHeight;
        private LinearGradient backgroundGradient;
        private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint paint = new Paint();
        private Rect blurBounds = new Rect();
        private boolean hasEmoji;
        private boolean emojiIsCollectible;
        public final AnimatedFloat emojiLoadedT = new AnimatedFloat(this, 0, 440, CubicBezierInterpolator.EASE_OUT_QUINT);
        public final AnimatedFloat emojiFullT = new AnimatedFloat(this, 0, 440, CubicBezierInterpolator.EASE_OUT_QUINT);
        private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable emoji = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(this, false, dp(20), AnimatedEmojiDrawable.CACHE_TYPE_ALERT_PREVIEW_STATIC);

        public ProfileContainerView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            emoji.attach();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            emoji.detach();
        }

        @Override
        public void setBackgroundColor(int color) {
            if (color != currentColor) {
                currentColor = color;
                paint.setColor(color);
                invalidate();
                if (!hasColorById) {
                    actionBarBackgroundColor = currentColor;
                }
            }
        }

        public void setBackgroundColorId(MessagesController.PeerColor peerColor, boolean animated) {
            if (peerColor != null) {
                hasColorById = true;
                color1 = peerColor.getBgColor1(Theme.isCurrentThemeDark());
                color2 = peerColor.getBgColor2(Theme.isCurrentThemeDark());

                actionBarBackgroundColor = ColorUtils.blendARGB(color1, color2, 0.25f);
                if (peerColor.patternColor != 0) {
                    emojiColor = peerColor.patternColor;
                } else {
                    emojiColor = PeerColorActivity.adaptProfileEmojiColor(color1);
                }
            } else {
                actionBarBackgroundColor = currentColor;
                hasColorById = false;
                if (AndroidUtilities.computePerceivedBrightness(getThemedColor(Theme.key_actionBarDefault)) > .8f) {
                    emojiColor = getThemedColor(Theme.key_windowBackgroundWhiteBlueText);
                } else if (AndroidUtilities.computePerceivedBrightness(getThemedColor(Theme.key_actionBarDefault)) < .2f) {
                    emojiColor = Theme.multAlpha(getThemedColor(Theme.key_actionBarDefaultTitle), .5f);
                } else {
                    emojiColor = PeerColorActivity.adaptProfileEmojiColor(getThemedColor(Theme.key_actionBarDefault));
                }
            }
            if (!animated) {
                color1Animated.set(color1, true);
                color2Animated.set(color2, true);
            }
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final int height = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
            //final float v = listOffset + height + searchTransitionOffset;
            final float v = listOffset + height;

            //int y1 = (int) (v * (1.0f - mediaHeaderAnimationProgress));
            int y1 = (int) v;

            if (y1 != 0) {
                if (previousTransitionFragment != null && previousTransitionFragment.getContentView() != null) {
                    blurBounds.set(0, 0, getMeasuredWidth(), y1);
                    if (previousTransitionFragment.getActionBar() != null && !previousTransitionFragment.getContentView().blurWasDrawn() && previousTransitionFragment.getActionBar().getBackground() == null) {
                        paint.setColor(Theme.getColor(Theme.key_actionBarDefault, previousTransitionFragment.getResourceProvider()));
                        canvas.drawRect(blurBounds, paint);
                    } else if (previousTransitionMainFragment != null && previousTransitionMainFragment instanceof DialogsActivity && previousTransitionMainFragment.getFragmentView() instanceof SizeNotifierFrameLayout) {
                        previousTransitionMainFragment.getActionBar().blurScrimPaint.setColor(Theme.getColor(Theme.key_actionBarDefault, previousTransitionMainFragment.getResourceProvider()));
                        ((SizeNotifierFrameLayout) previousTransitionMainFragment.getFragmentView()).drawBlurRect(canvas, getY(), blurBounds, previousTransitionMainFragment.getActionBar().blurScrimPaint, true);
                    } else {
                        previousTransitionFragment.getContentView().drawBlurRect(canvas, getY(), blurBounds, previousTransitionFragment.getActionBar().blurScrimPaint, true);
                    }
                }
                paint.setColor(currentColor);
                final int color1 = color1Animated.set(this.color1);
                final int color2 = color2Animated.set(this.color2);
                final int gradientHeight = AndroidUtilities.statusBarHeight + AndroidUtilities.dp(144);
                if (backgroundGradient == null || backgroundGradientColor1 != color1 || backgroundGradientColor2 != color2 || backgroundGradientHeight != gradientHeight) {
                    backgroundGradient = new LinearGradient(0, 0, 0, backgroundGradientHeight = gradientHeight, new int[] { backgroundGradientColor2 = color2, backgroundGradientColor1 = color1 }, new float[] { 0, 1 }, Shader.TileMode.CLAMP);
                    backgroundPaint.setShader(backgroundGradient);
                }
                //final float progressToGradient = (playProfileAnimation == 0 ? 1f : avatarAnimationProgress) * hasColorAnimated.set(hasColorById);
                final float progressToGradient = (avatarAnimationProgress) * hasColorAnimated.set(hasColorById);
                if (progressToGradient < 1) {
                    canvas.drawRect(0, 0, getMeasuredWidth(), y1, paint);
                }
                if (progressToGradient > 0) {
                    backgroundPaint.setAlpha((int) (0xFF * progressToGradient));
                    canvas.drawRect(0, 0, getMeasuredWidth(), y1, backgroundPaint);
                }
                if (hasEmoji) {
                    final float loadedScale = emojiLoadedT.set(isEmojiLoaded());
                    final float full = emojiFullT.set(emojiIsCollectible);
                    if (loadedScale > 0) {
                        canvas.save();
                        canvas.clipRect(0, 0, getMeasuredWidth(), y1);
                        StarGiftPatterns.drawProfilePattern(canvas, emoji, getMeasuredWidth(), ((actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + dp(144)) - (1f - listOffset / dp(88)) * dp(50), Math.min(1f, listOffset / dp(88)), full);
                        canvas.restore();
                    }
                }
                if (previousTransitionFragment != null) {
                    ActionBar actionBar = previousTransitionFragment.getActionBar();
                    ActionBarMenu menu = actionBar.menu;
                    if (actionBar != null && menu != null) {
                        int restoreCount = canvas.save();
                        canvas.translate(actionBar.getX() + menu.getX(), actionBar.getY() + menu.getY());
                        canvas.saveLayerAlpha(0, 0, menu.getMeasuredWidth(), menu.getMeasuredHeight(), (int) (255 * (1f - avatarAnimationProgress)), Canvas.ALL_SAVE_FLAG);
                        menu.draw(canvas);
                        canvas.restoreToCount(restoreCount);
                    }
                }
            }
            if (y1 != v) {
                int color = getThemedColor(Theme.key_windowBackgroundWhite);
                paint.setColor(color);
                blurBounds.set(0, y1, getMeasuredWidth(), (int) v);
                contentView.drawBlurRect(canvas, getY(), blurBounds, paint, true);
            }

            if (parentLayout != null) {
                parentLayout.drawHeaderShadow(canvas, (int) (headerShadowAlpha * 255), (int) v);
            }
        }

        private boolean emojiLoaded;
        private boolean isEmojiLoaded() {
            if (emojiLoaded) {
                return true;
            }
            if (emoji != null && emoji.getDrawable() instanceof AnimatedEmojiDrawable) {
                AnimatedEmojiDrawable drawable = (AnimatedEmojiDrawable) emoji.getDrawable();
                if (drawable.getImageReceiver() != null && drawable.getImageReceiver().hasImageLoaded()) {
                    return emojiLoaded = true;
                }
            }
            return false;
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

    private void onCallClick(boolean isVideoCall) {
        if (userId != 0) {
            TLRPC.User user = getMessagesController().getUser(userId);
            if (user != null) {
                VoIPHelper.startCall(user, isVideoCall, userInfo != null && userInfo.video_calls_available, getParentActivity(), userInfo, getAccountInstance());
            }
        } else if (chatId != 0) {
            ChatObject.Call call = getMessagesController().getGroupCall(chatId, false);
            if (call == null) {
                VoIPHelper.showGroupCallAlert(this, currentChat, null, false, getAccountInstance());
            } else {
                VoIPHelper.startCall(currentChat, null, null, false, getParentActivity(), this, getAccountInstance());
            }
        }
    }

    private void onMessageClick() {
        if (userId != 0) {
            finishFragment();
        } else {
            openDiscussion();
        }
    }

    private void onLeaveClick() {
        boolean isForum = ChatObject.isForum(currentChat);
        AlertsCreator.createClearOrDeleteDialogAlert(ProfileActivityV2.this, false, currentChat, null, false, isForum, !isForum, (param) -> {
            getNotificationCenter().removeObserver(ProfileActivityV2.this, NotificationCenter.closeChats);
            getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
            finishFragment();
            getNotificationCenter().postNotificationName(NotificationCenter.needDeleteDialog, -currentChat.id, null, currentChat, param);
        }, resourcesProvider);
    }

    private void openDiscussion() {
        if (chatInfo == null || chatInfo.linked_chat_id == 0) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("chat_id", chatInfo.linked_chat_id);
        if (!getMessagesController().checkCanOpenChat(args, ProfileActivityV2.this)) {
            return;
        }
        presentFragment(new ChatActivity(args));
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
                int childLeft = x;
                int childRight = childLeft + childWidth;

                child.layout(childLeft, 0, childRight, getMeasuredHeight());
                x += childWidth + itemSpace;
            }
        }

        public void addAction(int iconResId, String text, View.OnClickListener clickListener) {
            View actionView = createActionView(iconResId, text, clickListener);
            addView(actionView);
        }

        private View createActionView(int drawableResId, String text, View.OnClickListener clickListener) {
            Context context = getContext();
            LinearLayout container = new LinearLayout(context);
            container.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER);
            container.setClickable(true);
            container.setOnClickListener(clickListener);

            ImageView icon = new ImageView(context);
            icon.setImageDrawable(ContextCompat.getDrawable(context, drawableResId));

            TextView label = new TextView(context);
            label.setText(text);
            label.setGravity(Gravity.CENTER);
            label.setTextSize(14);
            label.setTextColor(getThemedColor(Theme.key_actionBarDefaultIcon));
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setColor(Color.argb(38, 0, 0, 0));
            background.setCornerRadius(AndroidUtilities.dp(12f));

            StateListDrawable stateListDrawable = new StateListDrawable();

            GradientDrawable pressedDrawable = new GradientDrawable();
            pressedDrawable.setShape(GradientDrawable.RECTANGLE);
            pressedDrawable.setColor(Color.argb(76, 0, 0, 0));
            pressedDrawable.setCornerRadius(AndroidUtilities.dp(12f));

            GradientDrawable defaultDrawable = new GradientDrawable();
            defaultDrawable.setShape(GradientDrawable.RECTANGLE);
            defaultDrawable.setColor(Color.argb(38, 0, 0, 0));
            defaultDrawable.setCornerRadius(AndroidUtilities.dp(12f));

            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
            stateListDrawable.addState(new int[]{}, defaultDrawable);

            container.setBackground(stateListDrawable);

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
