package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatPluralString;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.telegram.tgnet.TLRPC;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.Timer;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Stories.recorder.StoryRecorder;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class ProfileActivityV3 extends BaseFragment implements SharedMediaLayout.Delegate, SharedMediaLayout.SharedMediaPreloaderDelegate {

    private Theme.ResourcesProvider resourcesProvider;
    private SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader;
    private LinearLayoutManager layoutManager;
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    public SharedMediaLayout sharedMediaLayout;
    private boolean sharedMediaLayoutAttached;
    private SizeNotifierFrameLayout contentView;

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
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
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
        public int getItemViewType(int position) {
            if (position == 5) {
                return VIEW_TYPE_SHARED_MEDIA;
            }
            return super.getItemViewType(position);
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
            return 6;
        }
    }
}
