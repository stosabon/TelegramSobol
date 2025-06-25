package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.Timer;
import org.telegram.tgnet.tl.TL_fragment;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.Stories.recorder.StoryRecorder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class ProfileActivityV3 extends BaseFragment implements SharedMediaLayout.Delegate, SharedMediaLayout.SharedMediaPreloaderDelegate {

    private Theme.ResourcesProvider resourcesProvider;

    private SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader;
    public SharedMediaLayout sharedMediaLayout;
    private boolean sharedMediaLayoutAttached;

    private LinearLayoutManager layoutManager;
    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private CharacterStyle loadingSpan;
    private TextCell setAvatarCell;
    private int rowCount;
    private int setAvatarRow;
    private int setAvatarSectionRow;
    private int channelRow;
    private int channelDividerRow;
    private int numberSectionRow;
    private int numberRow;
    public int birthdayRow;
    private int setUsernameRow;
    private int bioRow;
    private int phoneSuggestionSectionRow;
    private int graceSuggestionRow;
    private int graceSuggestionSectionRow;
    private int phoneSuggestionRow;
    private int passwordSuggestionSectionRow;
    private int passwordSuggestionRow;
    private int settingsSectionRow;
    private int settingsSectionRow2;
    private int notificationRow;
    private int languageRow;
    private int privacyRow;
    private int dataRow;
    private int chatRow;
    private int filtersRow;
    private int liteModeRow;
    private int stickersRow;
    private int devicesRow;
    private int devicesSectionRow;
    private int helpHeaderRow;
    private int questionRow;
    private int faqRow;
    private int policyRow;
    private int helpSectionCell;
    private int debugHeaderRow;
    private int sendLogsRow;
    private int sendLastLogsRow;
    private int clearLogsRow;
    private int switchBackendRow;
    private int versionRow;
    private int emptyRow;
    private int bottomPaddingRow;
    private int infoHeaderRow;
    private int phoneRow;
    private int locationRow;
    private int userInfoRow;
    private int channelInfoRow;
    private int usernameRow;
    private int notificationsDividerRow;
    private int notificationsRow;
    private int bizHoursRow;
    private int bizLocationRow;
    private int notificationsSimpleRow;
    private int infoStartRow, infoEndRow;
    private int infoSectionRow;
    private int affiliateRow;
    private int infoAffiliateRow;
    private int sendMessageRow;
    private int reportRow;
    private int reportReactionRow;
    private int reportDividerRow;
    private int addToContactsRow;
    private int addToGroupButtonRow;
    private int addToGroupInfoRow;
    private int premiumRow;
    private int starsRow;
    private int businessRow;
    private int premiumGiftingRow;
    private int premiumSectionsRow;
    private int botAppRow;
    private int botPermissionsHeader;
    @Keep
    private int botPermissionLocation;
    @Keep
    private int botPermissionEmojiStatus;
    private int botPermissionEmojiStatusReqId;
    @Keep
    private int botPermissionBiometry;
    private int botPermissionsDivider;
    private int settingsTimerRow;
    private int settingsKeyRow;
    private int secretSettingsSectionRow;
    private int membersHeaderRow;
    private int membersStartRow;
    private int membersEndRow;
    private int addMemberRow;
    private int subscribersRow;
    private int subscribersRequestsRow;
    private int administratorsRow;
    private int settingsRow;
    private int botStarsBalanceRow;
    private int botTonBalanceRow;
    private int channelBalanceRow;
    private int channelBalanceSectionRow;
    private int balanceDividerRow;
    private int blockedUsersRow;
    private int membersSectionRow;
    private int sharedMediaRow;
    private int unblockRow;
    private int joinRow;
    private int lastSectionRow;

    private SizeNotifierFrameLayout contentView;
    private UndoView undoView;

    private long userId;
    private long chatId;
    private long topicId;
    private long dialogId;
    public boolean openCommonChats;
    public boolean openGifts;
    public boolean saved;
    private boolean openedGifts;
    private boolean openSimilar;
    public boolean myProfile;
    private TLRPC.UserFull userInfo;
    private TLRPC.ChatFull chatInfo;
    private TLRPC.Chat currentChat;
    private ArrayList<Integer> sortedUsers;

    public ProfileActivityV3(Bundle args) {
        this(args, null);
    }

    public ProfileActivityV3(Bundle args, SharedMediaLayout.SharedMediaPreloader preloader) {
        super(args);
        sharedMediaPreloader = preloader;
    }

    public static ProfileActivityV3 of(long dialogId) {
        Bundle bundle = new Bundle();
        if (dialogId >= 0) {
            bundle.putLong("user_id", dialogId);
        } else {
            bundle.putLong("chat_id", -dialogId);
        }
        return new ProfileActivityV3(bundle);
    }

    @Override
    public boolean onFragmentCreate() {
        Log.e("STAS", "onFragmentCreate");
        userId = arguments.getLong("user_id", 0);
        chatId = arguments.getLong("chat_id", 0);
        topicId = arguments.getLong("topic_id", 0);
        saved = arguments.getBoolean("saved", false);
        openSimilar = arguments.getBoolean("similar", false);
        openGifts = arguments.getBoolean("open_gifts", false);
        openCommonChats = arguments.getBoolean("open_common", false);
        myProfile = arguments.getBoolean("my_profile", false);
        if (userId != 0) {
            dialogId = arguments.getLong("dialog_id", 0);
            userInfo = getMessagesController().getUserFull(userId);
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
            sortedUsers = new ArrayList<>();
            if (chatInfo == null) {
                chatInfo = getMessagesController().getChatFull(chatId);
            }
            if (ChatObject.isChannel(currentChat)) {
                getMessagesController().loadFullChat(chatId, classGuid, true);
            } else if (chatInfo == null) {
                chatInfo = getMessagesStorage().loadChatInfo(chatId, false, null, false, false);
            }
        }
        if (sharedMediaPreloader == null) {
            sharedMediaPreloader = new SharedMediaLayout.SharedMediaPreloader(this);
        }
        sharedMediaPreloader.addDelegate(this);

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (sharedMediaLayout != null) {
            sharedMediaLayout.onDestroy();
        }
        if (sharedMediaPreloader != null) {
            sharedMediaPreloader.onDestroy(this);
        }
        if (sharedMediaPreloader != null) {
            sharedMediaPreloader.removeDelegate(this);
        }
    }

    @Override
    public View createView(Context context) {
        Log.e("STAS", "createView" + " sharedMediaLayout = " + sharedMediaLayout);

        BaseFragment lastFragment = parentLayout.getLastFragment();
        if (lastFragment instanceof ChatActivity && ((ChatActivity) lastFragment).themeDelegate != null && ((ChatActivity) lastFragment).themeDelegate.getCurrentTheme() != null) {
            resourcesProvider = lastFragment.getResourceProvider();
        }
        fragmentView = new SizeNotifierFrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                Log.e("STAS", "onMeasure");
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                super.onLayout(changed, l, t, r, b);
                Log.e("STAS", "onLayout");
            }
        };
        fragmentView.setWillNotDraw(false);
        contentView = ((SizeNotifierFrameLayout) fragmentView);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        initSharedMediaLayout(context);
        initListView(context);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        undoView = new UndoView(context, null, false, resourcesProvider);
        frameLayout.addView(undoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        // test region
        listAdapter.notifyDataSetChanged();
        // end test region
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sharedMediaLayout != null) {
            sharedMediaLayout.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sharedMediaLayout != null) {
            sharedMediaLayout.onPause();
        }
    }

    private void initListView(Context context) {
        listAdapter = new ListAdapter(context);
        layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView = new RecyclerListView(context);
        listView.setAdapter(listAdapter);
        listView.setLayoutManager(layoutManager);
    }

    private void initSharedMediaLayout(Context context) {
        final ArrayList<Integer> users = chatInfo != null && chatInfo.participants != null && chatInfo.participants.participants.size() > 5 ? sortedUsers : null;
        int initialTab = -1;
        if (openCommonChats) {
            initialTab = SharedMediaLayout.TAB_COMMON_GROUPS;
        } else if (openGifts && (userInfo != null && userInfo.stargifts_count > 0 || chatInfo != null && chatInfo.stargifts_count > 0)) {
            initialTab = SharedMediaLayout.TAB_GIFTS;
            openedGifts = true;
        } else if (openSimilar) {
            initialTab = SharedMediaLayout.TAB_RECOMMENDED_CHANNELS;
        } else if (users != null) {
            initialTab = SharedMediaLayout.TAB_GROUPUSERS;
        }
        sharedMediaLayout = new SharedMediaLayout(context, getDialogId(), sharedMediaPreloader, userInfo != null ? userInfo.common_chats_count : 0, sortedUsers, chatInfo, userInfo, initialTab, this, this, SharedMediaLayout.VIEW_TYPE_PROFILE_ACTIVITY, resourcesProvider) {

            @Override
            protected boolean includeSavedDialogs() {
                return dialogId == getUserConfig().getClientUserId() && !saved;
            }
            @Override
            protected boolean isSelf() {
                return myProfile;
            }

            @Override
            protected boolean isStoriesView() {
                return myProfile;
            }

            @Override
            protected int getInitialTab() {
                return TAB_STORIES;
            }
        };
        sharedMediaLayout.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void scrollToSharedMedia() {

    }

    @Override
    public boolean onMemberClick(TLRPC.ChatParticipant participant, boolean b, boolean resultOnly, View view) {
        return false;
    }

    @Override
    public TLRPC.Chat getCurrentChat() {
        return currentChat;
    }

    @Override
    public boolean isFragmentOpened() {
        return false;
    }

    @Override
    public RecyclerListView getListView() {
        return listView;
    }

    @Override
    public boolean canSearchMembers() {
        return false;
    }

    @Override
    public void updateSelectedMediaTabText() {

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

    @Override
    public void mediaCountUpdated() {

    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final static int VIEW_TYPE_HEADER = 1,
                VIEW_TYPE_TEXT_DETAIL = 2,
                VIEW_TYPE_ABOUT_LINK = 3,
                VIEW_TYPE_TEXT = 4,
                VIEW_TYPE_DIVIDER = 5,
                VIEW_TYPE_NOTIFICATIONS_CHECK = 6,
                VIEW_TYPE_SHADOW = 7,
                VIEW_TYPE_USER = 8,
                VIEW_TYPE_EMPTY = 11,
                VIEW_TYPE_BOTTOM_PADDING = 12,
                VIEW_TYPE_SHARED_MEDIA = 13,
                VIEW_TYPE_VERSION = 14,
                VIEW_TYPE_SUGGESTION = 15,
                VIEW_TYPE_ADDTOGROUP_INFO = 17,
                VIEW_TYPE_PREMIUM_TEXT_CELL = 18,
                VIEW_TYPE_TEXT_DETAIL_MULTILINE = 19,
                VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE = 20,
                VIEW_TYPE_LOCATION = 21,
                VIEW_TYPE_HOURS = 22,
                VIEW_TYPE_CHANNEL = 23,
                VIEW_TYPE_STARS_TEXT_CELL = 24,
                VIEW_TYPE_BOT_APP = 25,
                VIEW_TYPE_SHADOW_TEXT = 26,
                VIEW_TYPE_COLORFUL_TEXT = 27;

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            if (holder.getAdapterPosition() == setAvatarRow) {
                setAvatarCell = null;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            if (notificationRow != -1) {
                int position = holder.getAdapterPosition();
                return position == notificationRow || position == numberRow || position == privacyRow ||
                        position == languageRow || position == setUsernameRow || position == bioRow ||
                        position == versionRow || position == dataRow || position == chatRow ||
                        position == questionRow || position == devicesRow || position == filtersRow || position == stickersRow ||
                        position == faqRow || position == policyRow || position == sendLogsRow || position == sendLastLogsRow ||
                        position == clearLogsRow || position == switchBackendRow || position == setAvatarRow ||
                        position == addToGroupButtonRow || position == premiumRow || position == premiumGiftingRow ||
                        position == businessRow || position == liteModeRow || position == birthdayRow || position == channelRow ||
                        position == starsRow;
            }
            if (holder.itemView instanceof UserCell) {
                UserCell userCell = (UserCell) holder.itemView;
                Object object = userCell.getCurrentObject();
                if (object instanceof TLRPC.User) {
                    TLRPC.User user = (TLRPC.User) object;
                    if (UserObject.isUserSelf(user)) {
                        return false;
                    }
                }
            }
            int type = holder.getItemViewType();
            return type != VIEW_TYPE_HEADER && type != VIEW_TYPE_DIVIDER && type != VIEW_TYPE_SHADOW &&
                    type != VIEW_TYPE_EMPTY && type != VIEW_TYPE_BOTTOM_PADDING && type != VIEW_TYPE_SHARED_MEDIA &&
                    type != 9 && type != 10 && type != VIEW_TYPE_BOT_APP; // These are legacy ones, left for compatibility
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_SHARED_MEDIA) {
                if (sharedMediaLayout.getParent() != null) {
                    ((ViewGroup) sharedMediaLayout.getParent()).removeView(sharedMediaLayout);
                }
                view = sharedMediaLayout;
            } else {
                view = new HeaderCell(mContext, 23, resourcesProvider);
                view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            }
            if (viewType != VIEW_TYPE_SHARED_MEDIA) {
                view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.itemView instanceof  HeaderCell) {
                HeaderCell headerCell = (HeaderCell) holder.itemView;
                headerCell.setText(position + "");
                headerCell.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
            }
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            if (holder.itemView == sharedMediaLayout) {
                sharedMediaLayoutAttached = true;
            }
            if (holder.itemView instanceof TextDetailCell) {
                //((TextDetailCell) holder.itemView).textView.setLoading(loadingSpan);
                //((TextDetailCell) holder.itemView).valueTextView.setLoading(loadingSpan);
            }
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            if (holder.itemView == sharedMediaLayout) {
                sharedMediaLayoutAttached = false;
            }
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == infoHeaderRow || position == membersHeaderRow || position == settingsSectionRow2 ||
                    position == numberSectionRow || position == helpHeaderRow || position == debugHeaderRow || position == botPermissionsHeader) {
                return VIEW_TYPE_HEADER;
            } else if (position == phoneRow || position == locationRow || position == numberRow || position == birthdayRow) {
                return VIEW_TYPE_TEXT_DETAIL;
            } else if (position == usernameRow || position == setUsernameRow) {
                return VIEW_TYPE_TEXT_DETAIL_MULTILINE;
            } else if (position == userInfoRow || position == channelInfoRow || position == bioRow) {
                return VIEW_TYPE_ABOUT_LINK;
            } else if (position == settingsTimerRow || position == settingsKeyRow || position == reportRow || position == reportReactionRow ||
                    position == subscribersRow || position == subscribersRequestsRow || position == administratorsRow || position == settingsRow || position == blockedUsersRow ||
                    position == addMemberRow || position == joinRow || position == unblockRow ||
                    position == sendMessageRow || position == notificationRow || position == privacyRow ||
                    position == languageRow || position == dataRow || position == chatRow ||
                    position == questionRow || position == devicesRow || position == filtersRow || position == stickersRow ||
                    position == faqRow || position == policyRow || position == sendLogsRow || position == sendLastLogsRow ||
                    position == clearLogsRow || position == switchBackendRow || position == setAvatarRow || position == addToGroupButtonRow ||
                    position == addToContactsRow || position == liteModeRow || position == premiumGiftingRow || position == businessRow || position == botStarsBalanceRow || position == botTonBalanceRow || position == channelBalanceRow || position == botPermissionLocation || position == botPermissionBiometry || position == botPermissionEmojiStatus) {
                return VIEW_TYPE_TEXT;
            } else if (position == notificationsDividerRow) {
                return VIEW_TYPE_DIVIDER;
            } else if (position == notificationsRow) {
                return VIEW_TYPE_NOTIFICATIONS_CHECK;
            } else if (position == notificationsSimpleRow) {
                return VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE;
            } else if (position == lastSectionRow || position == membersSectionRow ||
                    position == secretSettingsSectionRow || position == settingsSectionRow || position == devicesSectionRow ||
                    position == helpSectionCell || position == setAvatarSectionRow || position == passwordSuggestionSectionRow ||
                    position == phoneSuggestionSectionRow || position == premiumSectionsRow || position == reportDividerRow ||
                    position == channelDividerRow || position == graceSuggestionSectionRow || position == balanceDividerRow ||
                    position == botPermissionsDivider || position == channelBalanceSectionRow
            ) {
                return VIEW_TYPE_SHADOW;
            } else if (position >= membersStartRow && position < membersEndRow) {
                return VIEW_TYPE_USER;
            } else if (position == emptyRow) {
                return VIEW_TYPE_EMPTY;
            } else if (position == bottomPaddingRow) {
                return VIEW_TYPE_BOTTOM_PADDING;
            } else if (position == sharedMediaRow) {
                return VIEW_TYPE_SHARED_MEDIA;
            } else if (position == versionRow) {
                return VIEW_TYPE_VERSION;
            } else if (position == passwordSuggestionRow || position == phoneSuggestionRow || position == graceSuggestionRow) {
                return VIEW_TYPE_SUGGESTION;
            } else if (position == addToGroupInfoRow) {
                return VIEW_TYPE_ADDTOGROUP_INFO;
            } else if (position == premiumRow) {
                return VIEW_TYPE_PREMIUM_TEXT_CELL;
            } else if (position == starsRow) {
                return VIEW_TYPE_STARS_TEXT_CELL;
            } else if (position == bizLocationRow) {
                return VIEW_TYPE_LOCATION;
            } else if (position == bizHoursRow) {
                return VIEW_TYPE_HOURS;
            } else if (position == channelRow) {
                return VIEW_TYPE_CHANNEL;
            } else if (position == botAppRow) {
                return VIEW_TYPE_BOT_APP;
            } else if (position == infoSectionRow || position == infoAffiliateRow) {
                return VIEW_TYPE_SHADOW_TEXT;
            } else if (position == affiliateRow) {
                return VIEW_TYPE_COLORFUL_TEXT;
            }
            return 0;
        }

        private final HashMap<TLRPC.TL_username, ClickableSpan> usernameSpans = new HashMap<TLRPC.TL_username, ClickableSpan>();
        private ClickableSpan makeUsernameLinkSpan(TLRPC.TL_username usernameObj) {
            ClickableSpan span = usernameSpans.get(usernameObj);
            if (span != null) return span;

            final String usernameRaw = usernameObj.username;
            span = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    if (!usernameObj.editable) {
                        if (loadingSpan == this) return;
                        setLoadingSpan(this);
                        TL_fragment.TL_getCollectibleInfo req = new TL_fragment.TL_getCollectibleInfo();
                        TL_fragment.TL_inputCollectibleUsername input = new TL_fragment.TL_inputCollectibleUsername();
                        input.username = usernameObj.username;
                        req.collectible = input;
                        int reqId = getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                            setLoadingSpan(null);
                            if (res instanceof TL_fragment.TL_collectibleInfo) {
                                TLObject obj;
                                if (userId != 0) {
                                    obj = getMessagesController().getUser(userId);
                                } else {
                                    obj = getMessagesController().getChat(chatId);
                                }
                                if (getContext() == null) {
                                    return;
                                }
                                FragmentUsernameBottomSheet.open(getContext(), FragmentUsernameBottomSheet.TYPE_USERNAME, usernameObj.username, obj, (TL_fragment.TL_collectibleInfo) res, getResourceProvider());
                            } else {
                                BulletinFactory.showError(err);
                            }
                        }));
                        getConnectionsManager().bindRequestToGuid(reqId, getClassGuid());
                    } else {
                        setLoadingSpan(null);
                        String urlFinal = getMessagesController().linkPrefix + "/" + usernameRaw;
                        if (currentChat == null || !currentChat.noforwards) {
                            AndroidUtilities.addToClipboard(urlFinal);
                            undoView.showWithAction(0, UndoView.ACTION_USERNAME_COPIED, null);
                        }
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setUnderlineText(false);
                    ds.setColor(ds.linkColor);
                }
            };
            usernameSpans.put(usernameObj, span);
            return span;
        }

        private CharSequence alsoUsernamesString(String originalUsername, ArrayList<TLRPC.TL_username> alsoUsernames, CharSequence fallback) {
            if (alsoUsernames == null) {
                return fallback;
            }
            alsoUsernames = new ArrayList<>(alsoUsernames);
            for (int i = 0; i < alsoUsernames.size(); ++i) {
                if (
                        !alsoUsernames.get(i).active ||
                                originalUsername != null && originalUsername.equals(alsoUsernames.get(i).username)
                ) {
                    alsoUsernames.remove(i--);
                }
            }
            if (alsoUsernames.size() > 0) {
                SpannableStringBuilder usernames = new SpannableStringBuilder();
                for (int i = 0; i < alsoUsernames.size(); ++i) {
                    TLRPC.TL_username usernameObj = alsoUsernames.get(i);
                    final String usernameRaw = usernameObj.username;
                    SpannableString username = new SpannableString("@" + usernameRaw);
                    username.setSpan(makeUsernameLinkSpan(usernameObj), 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    username.setSpan(new ForegroundColorSpan(getThemedColor(Theme.key_chat_messageLinkIn)), 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    usernames.append(username);
                    if (i < alsoUsernames.size() - 1) {
                        usernames.append(", ");
                    }
                }
                String string = getString(R.string.UsernameAlso);
                SpannableStringBuilder finalString = new SpannableStringBuilder(string);
                final String toFind = "%1$s";
                int index = string.indexOf(toFind);
                if (index >= 0) {
                    finalString.replace(index, index + toFind.length(), usernames);
                }
                return finalString;
            } else {
                return fallback;
            }
        }
    }

    private void setLoadingSpan(CharacterStyle span) {
        if (loadingSpan == span) return;
        loadingSpan = span;
        AndroidUtilities.forEachViews(listView, view -> {
            if (view instanceof TextDetailCell) {
                ((TextDetailCell) view).textView.setLoading(loadingSpan);
                ((TextDetailCell) view).valueTextView.setLoading(loadingSpan);
            }
        });
    }
}
