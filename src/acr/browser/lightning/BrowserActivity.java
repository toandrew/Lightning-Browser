/*
 * Copyright 2014 A.C.R. Development
 */

package acr.browser.lightning;

import info.guardianproject.onionkit.ui.OrbotHelper;
import info.guardianproject.onionkit.web.WebkitProxy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import tv.matchstick.flint.ApplicationMetadata;
import tv.matchstick.flint.ConnectionResult;
import tv.matchstick.flint.Flint;
import tv.matchstick.flint.Flint.ApplicationConnectionResult;
import tv.matchstick.flint.FlintDevice;
import tv.matchstick.flint.FlintManager;
import tv.matchstick.flint.FlintMediaControlIntent;
import tv.matchstick.flint.MediaInfo;
import tv.matchstick.flint.MediaMetadata;
import tv.matchstick.flint.MediaStatus;
import tv.matchstick.flint.RemoteMediaPlayer;
import tv.matchstick.flint.RemoteMediaPlayer.MediaChannelResult;
import tv.matchstick.flint.ResultCallback;
import tv.matchstick.flint.Status;
import tv.matchstick.flint.images.WebImage;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebIconDatabase;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.VideoView;

public class BrowserActivity extends FragmentActivity implements
        BrowserController {
    private static final String TAG = "BrowserActivity";

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListLeft;
    private RelativeLayout mDrawerLeft;
    private LinearLayout mDrawerRight;
    private ListView mDrawerListRight;
    private RelativeLayout mNewTab;
    private ActionBarDrawerToggle mDrawerToggle;
    private List<LightningView> mWebViews = new ArrayList<LightningView>();
    private LightningView mCurrentView;
    private int mIdGenerator;
    private LightningViewAdapter mTitleAdapter;
    private List<HistoryItem> mBookmarkList;
    private BookmarkViewAdapter mBookmarkAdapter;
    private AutoCompleteTextView mSearch;
    private ClickHandler mClickHandler;
    private ProgressBar mProgressBar;
    private boolean mSystemBrowser = false;
    private ValueCallback<Uri> mUploadMessage;
    private View mCustomView;
    private int mOriginalOrientation;
    private int mActionBarSize;
    private ActionBar mActionBar;
    private boolean mFullScreen;
    private FrameLayout mBrowserFrame;
    private FullscreenHolder mFullscreenContainer;
    private CustomViewCallback mCustomViewCallback;
    private final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);
    private Bitmap mDefaultVideoPoster;
    private View mVideoProgressView;
    private HistoryDatabaseHandler mHistoryHandler;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditPrefs;
    private Context mContext;
    private Bitmap mWebpageBitmap;
    private String mSearchText;
    private Activity mActivity;
    private final int API = android.os.Build.VERSION.SDK_INT;
    private Drawable mDeleteIcon;
    private Drawable mRefreshIcon;
    private Drawable mCopyIcon;
    private Drawable mIcon;
    private int mActionBarSizeDp;
    private int mNumberIconColor;
    private String mHomepage;
    private boolean mIsNewIntent = false;
    private VideoView mVideoView;
    private static SearchAdapter mSearchAdapter;
    private static LayoutParams mMatchParent = new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    private BookmarkManager mBookmarkManager;

    // used to send cust messages.
    private FlintMsgChannel mFlintMsgChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.e(TAG, "onStart");
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.e(TAG, "onStop");

        mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    @SuppressWarnings("deprecation")
    private synchronized void initialize() {
        setContentView(R.layout.activity_main);
        TypedValue typedValue = new TypedValue();
        Theme theme = getTheme();
        theme.resolveAttribute(R.attr.numberColor, typedValue, true);
        mNumberIconColor = typedValue.data;
        mPreferences = getSharedPreferences(PreferenceConstants.PREFERENCES, 0);
        mEditPrefs = mPreferences.edit();
        mContext = this;
        if (mWebViews != null) {
            mWebViews.clear();
        } else {
            mWebViews = new ArrayList<LightningView>();
        }
        mBookmarkManager = new BookmarkManager(this);
        if (!mPreferences.getBoolean(
                PreferenceConstants.OLD_BOOKMARKS_IMPORTED, false)) {
            List<HistoryItem> old = Utils.getOldBookmarks(this);
            mBookmarkManager.addBookmarkList(old);
            mEditPrefs.putBoolean(PreferenceConstants.OLD_BOOKMARKS_IMPORTED,
                    true).apply();
        }
        mActivity = this;
        mClickHandler = new ClickHandler(this);
        mBrowserFrame = (FrameLayout) findViewById(R.id.content_frame);
        mProgressBar = (ProgressBar) findViewById(R.id.activity_bar);
        mProgressBar.setVisibility(View.GONE);
        mNewTab = (RelativeLayout) findViewById(R.id.new_tab_button);
        mDrawerLeft = (RelativeLayout) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerListLeft = (ListView) findViewById(R.id.left_drawer_list);
        mDrawerListLeft.setDivider(null);
        mDrawerListLeft.setDividerHeight(0);
        mDrawerRight = (LinearLayout) findViewById(R.id.right_drawer);
        mDrawerListRight = (ListView) findViewById(R.id.right_drawer_list);
        mDrawerListRight.setDivider(null);
        mDrawerListRight.setDividerHeight(0);
        setNavigationDrawerWidth();
        mWebpageBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_webpage);
        mActionBar = getActionBar();
        final TypedArray styledAttributes = mContext.getTheme()
                .obtainStyledAttributes(
                        new int[] { android.R.attr.actionBarSize });
        mActionBarSize = (int) styledAttributes.getDimension(0, 0);
        if (pixelsToDp(mActionBarSize) < 48) {
            mActionBarSize = Utils.convertToDensityPixels(mContext, 48);
        }
        mActionBarSizeDp = pixelsToDp(mActionBarSize);
        styledAttributes.recycle();

        mHomepage = mPreferences.getString(PreferenceConstants.HOMEPAGE,
                Constants.HOMEPAGE);

        mTitleAdapter = new LightningViewAdapter(this, R.layout.tab_list_item,
                mWebViews);
        mDrawerListLeft.setAdapter(mTitleAdapter);
        mDrawerListLeft.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerListLeft
                .setOnItemLongClickListener(new DrawerItemLongClickListener());

        mBookmarkList = mBookmarkManager.getBookmarks(true);
        mBookmarkAdapter = new BookmarkViewAdapter(this,
                R.layout.bookmark_list_item, mBookmarkList);
        mDrawerListRight.setAdapter(mBookmarkAdapter);
        mDrawerListRight
                .setOnItemClickListener(new BookmarkItemClickListener());
        mDrawerListRight
                .setOnItemLongClickListener(new BookmarkItemLongClickListener());

        if (mHistoryHandler == null) {
            mHistoryHandler = new HistoryDatabaseHandler(this);
        } else if (!mHistoryHandler.isOpen()) {
            mHistoryHandler = new HistoryDatabaseHandler(this);
        }

        // set display options of the ActionBar
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setCustomView(R.layout.search);

        RelativeLayout back = (RelativeLayout) findViewById(R.id.action_back);
        RelativeLayout forward = (RelativeLayout) findViewById(R.id.action_forward);
        if (back != null) {
            back.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mCurrentView != null) {
                        if (mCurrentView.canGoBack()) {
                            mCurrentView.goBack();
                        } else {
                            deleteTab(mDrawerListLeft.getCheckedItemPosition());
                        }
                    }
                }

            });
        }
        if (forward != null) {
            forward.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mCurrentView != null) {
                        if (mCurrentView.canGoForward()) {
                            mCurrentView.goForward();
                        }
                    }
                }

            });
        }

        // create the search EditText in the ActionBar
        mSearch = (AutoCompleteTextView) mActionBar.getCustomView()
                .findViewById(R.id.search);
        mDeleteIcon = getResources().getDrawable(R.drawable.ic_action_delete);
        mDeleteIcon.setBounds(0, 0, Utils.convertToDensityPixels(mContext, 24),
                Utils.convertToDensityPixels(mContext, 24));
        mRefreshIcon = getResources().getDrawable(R.drawable.ic_action_refresh);
        mRefreshIcon.setBounds(0, 0,
                Utils.convertToDensityPixels(mContext, 24),
                Utils.convertToDensityPixels(mContext, 24));
        mCopyIcon = getResources().getDrawable(R.drawable.ic_action_copy);
        mCopyIcon.setBounds(0, 0, Utils.convertToDensityPixels(mContext, 24),
                Utils.convertToDensityPixels(mContext, 24));
        mIcon = mRefreshIcon;
        mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
        mSearch.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View arg0, int arg1, KeyEvent arg2) {

                switch (arg1) {
                case KeyEvent.KEYCODE_ENTER:
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
                    searchTheWeb(mSearch.getText().toString());
                    if (mCurrentView != null) {
                        mCurrentView.requestFocus();
                    }
                    return true;
                default:
                    break;
                }
                return false;
            }

        });
        mSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && mCurrentView != null) {
                    if (mCurrentView != null) {
                        if (mCurrentView.getProgress() < 100) {
                            setIsLoading();
                        } else {
                            setIsFinishedLoading();
                        }
                    }
                    updateUrl(mCurrentView.getUrl());
                } else if (hasFocus) {
                    mIcon = mCopyIcon;
                    mSearch.setCompoundDrawables(null, null, mCopyIcon, null);
                }
            }
        });
        mSearch.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView arg0, int actionId,
                    KeyEvent arg2) {
                // hide the keyboard and search the web when the enter key
                // button is pressed
                if (actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_SEND
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || (arg2.getAction() == KeyEvent.KEYCODE_ENTER)) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
                    searchTheWeb(mSearch.getText().toString());
                    if (mCurrentView != null) {
                        mCurrentView.requestFocus();
                    }
                    return true;
                }
                return false;
            }

        });

        mSearch.setOnTouchListener(new OnTouchListener() {

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mSearch.getCompoundDrawables()[2] != null) {
                    boolean tappedX = event.getX() > (mSearch.getWidth()
                            - mSearch.getPaddingRight() - mIcon
                            .getIntrinsicWidth());
                    if (tappedX) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            if (mSearch.hasFocus()) {
                                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("label",
                                        mSearch.getText().toString());
                                clipboard.setPrimaryClip(clip);
                                Utils.showToast(
                                        mContext,
                                        mContext.getResources().getString(
                                                R.string.message_text_copied));
                            } else {
                                refreshOrStop();
                            }
                        }
                        return true;
                    }
                }
                return false;
            }

        });

        mSystemBrowser = getSystemBrowser();
        Thread initialize = new Thread(new Runnable() {

            @Override
            public void run() {
                initializeSearchSuggestions(mSearch);
            }

        });
        initialize.run();
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
        mDrawerLayout, /* DrawerLayout object */
        R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
        R.string.drawer_open, /* "open drawer" description for accessibility */
        R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (view.equals(mDrawerLeft)) {
                    mDrawerLayout.setDrawerLockMode(
                            DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerRight);
                } else if (view.equals(mDrawerRight)) {
                    mDrawerLayout.setDrawerLockMode(
                            DrawerLayout.LOCK_MODE_UNLOCKED, mDrawerLeft);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (drawerView.equals(mDrawerLeft)) {
                    mDrawerLayout.closeDrawer(mDrawerRight);
                    mDrawerLayout.setDrawerLockMode(
                            DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerRight);
                } else if (drawerView.equals(mDrawerRight)) {
                    mDrawerLayout.closeDrawer(mDrawerLeft);
                    mDrawerLayout.setDrawerLockMode(
                            DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerLeft);
                }
            }

        };

        mNewTab.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                newTab(null, true);
            }

        });

        mNewTab.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                String url = mPreferences.getString(
                        PreferenceConstants.SAVE_URL, null);
                if (url != null) {
                    newTab(url, true);
                    Toast.makeText(mContext, R.string.deleted_tab,
                            Toast.LENGTH_SHORT).show();
                }
                mEditPrefs.putString(PreferenceConstants.SAVE_URL, null)
                        .apply();
                return true;
            }

        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_right_shadow,
                GravityCompat.END);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_left_shadow,
                GravityCompat.START);
        initializePreferences();
        initializeTabs();

        if (API < 19) {
            WebIconDatabase.getInstance().open(
                    getDir("icons", MODE_PRIVATE).getPath());
        }

        checkForTor();

        initFlingServerSocket();

    }

    /*
     * If Orbot/Tor is installed, prompt the user if they want to enable
     * proxying for this session
     */
    public boolean checkForTor() {
        boolean useProxy = mPreferences.getBoolean(
                PreferenceConstants.USE_PROXY, false);

        OrbotHelper oh = new OrbotHelper(this);
        if (oh.isOrbotInstalled()
                && !mPreferences.getBoolean(
                        PreferenceConstants.INITIAL_CHECK_FOR_TOR, false)) {
            mEditPrefs.putBoolean(PreferenceConstants.INITIAL_CHECK_FOR_TOR,
                    true);
            mEditPrefs.apply();
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mPreferences
                                .edit()
                                .putBoolean(PreferenceConstants.USE_PROXY, true)
                                .apply();

                        initializeTor();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        mPreferences
                                .edit()
                                .putBoolean(PreferenceConstants.USE_PROXY,
                                        false).apply();
                        break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.use_tor_prompt)
                    .setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.no, dialogClickListener).show();

            return true;
        } else if (oh.isOrbotInstalled() & useProxy == true) {
            initializeTor();
            return true;
        } else {
            mEditPrefs.putBoolean(PreferenceConstants.USE_PROXY, false);
            mEditPrefs.apply();
            return false;
        }
    }

    /*
     * Initialize WebKit Proxying for Tor
     */
    public void initializeTor() {

        OrbotHelper oh = new OrbotHelper(this);
        if (!oh.isOrbotRunning()) {
            oh.requestOrbotStart(this);
        }
        try {
            String host = mPreferences.getString(
                    PreferenceConstants.USE_PROXY_HOST, "localhost");
            int port = mPreferences.getInt(PreferenceConstants.USE_PROXY_PORT,
                    8118);
            WebkitProxy.setProxy("acr.browser.lightning.BrowserApp",
                    getApplicationContext(), host, port);
        } catch (Exception e) {
            Log.d(Constants.TAG, "error enabling web proxying", e);
        }

    }

    public void setNavigationDrawerWidth() {
        int width = getResources().getDisplayMetrics().widthPixels * 3 / 4;
        int maxWidth = Utils.convertToDensityPixels(mContext, 300);
        if (width > maxWidth) {
            DrawerLayout.LayoutParams params = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerLeft
                    .getLayoutParams();
            params.width = maxWidth;
            mDrawerLeft.setLayoutParams(params);
            DrawerLayout.LayoutParams paramsRight = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerRight
                    .getLayoutParams();
            paramsRight.width = maxWidth;
            mDrawerRight.setLayoutParams(paramsRight);
        } else {
            DrawerLayout.LayoutParams params = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerLeft
                    .getLayoutParams();
            params.width = width;
            mDrawerLeft.setLayoutParams(params);
            DrawerLayout.LayoutParams paramsRight = (android.support.v4.widget.DrawerLayout.LayoutParams) mDrawerRight
                    .getLayoutParams();
            paramsRight.width = width;
            mDrawerRight.setLayoutParams(paramsRight);
        }
    }

    /*
     * Override this class
     */
    public synchronized void initializeTabs() {

    }

    public void restoreOrNewTab() {
        mIdGenerator = 0;

        String url = null;
        if (getIntent() != null) {
            url = getIntent().getDataString();
            if (url != null) {
                if (url.startsWith(Constants.FILE)) {
                    Utils.showToast(
                            this,
                            getResources().getString(
                                    R.string.message_blocked_local));
                    url = null;
                }
            }
        }
        if (mPreferences.getBoolean(PreferenceConstants.RESTORE_LOST_TABS,
                false)) {
            String mem = mPreferences.getString(PreferenceConstants.URL_MEMORY,
                    "");
            mEditPrefs.putString(PreferenceConstants.URL_MEMORY, "");
            String[] array = Utils.getArray(mem);
            int count = 0;
            for (int n = 0; n < array.length; n++) {
                if (array[n].length() > 0) {
                    newTab(array[n], true);
                    count++;
                }
            }
            if (url != null) {
                newTab(url, true);
            } else if (count == 0) {
                newTab(null, true);
            }
        } else {
            newTab(url, true);
        }
    }

    public void initializePreferences() {
        if (mPreferences == null) {
            mPreferences = getSharedPreferences(
                    PreferenceConstants.PREFERENCES, 0);
        }
        mFullScreen = mPreferences.getBoolean(PreferenceConstants.FULL_SCREEN,
                false);
        if (mPreferences.getBoolean(PreferenceConstants.HIDE_STATUS_BAR, false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        switch (mPreferences.getInt(PreferenceConstants.SEARCH, 1)) {
        case 0:
            mSearchText = mPreferences.getString(
                    PreferenceConstants.SEARCH_URL, Constants.GOOGLE_SEARCH);
            if (!mSearchText.startsWith(Constants.HTTP)
                    && !mSearchText.startsWith(Constants.HTTPS)) {
                mSearchText = Constants.GOOGLE_SEARCH;
            }
            break;
        case 1:
            mSearchText = Constants.GOOGLE_SEARCH;
            break;
        case 2:
            mSearchText = Constants.ANDROID_SEARCH;
            break;
        case 3:
            mSearchText = Constants.BING_SEARCH;
            break;
        case 4:
            mSearchText = Constants.YAHOO_SEARCH;
            break;
        case 5:
            mSearchText = Constants.STARTPAGE_SEARCH;
            break;
        case 6:
            mSearchText = Constants.STARTPAGE_MOBILE_SEARCH;
            break;
        case 7:
            mSearchText = Constants.DUCK_SEARCH;
            break;
        case 8:
            mSearchText = Constants.DUCK_LITE_SEARCH;
            break;
        case 9:
            mSearchText = Constants.BAIDU_SEARCH;
            break;
        case 10:
            mSearchText = Constants.YANDEX_SEARCH;
            break;
        }

        updateCookiePreference();
        if (mPreferences.getBoolean(PreferenceConstants.USE_PROXY, false)) {
            initializeTor();
        } else {
            try {
                WebkitProxy.resetProxy("acr.browser.lightning.BrowserApp",
                        getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * Override this if class overrides BrowserActivity
     */
    public void updateCookiePreference() {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mSearch.hasFocus()) {
                searchTheWeb(mSearch.getText().toString());
            }
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (mMediaPlayer != null && mMediaFlingBar != null
                    && mMediaFlingBar.getVisibility() == View.VISIBLE) {
                onVolumeChange(0.1);
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mMediaPlayer != null && mMediaFlingBar != null
                    && mMediaFlingBar.getVisibility() == View.VISIBLE) {
                onVolumeChange(-0.1);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
                mDrawerLayout.closeDrawer(mDrawerRight);
                mDrawerLayout.openDrawer(mDrawerLeft);
            } else if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
                mDrawerLayout.closeDrawer(mDrawerLeft);
            }
            mDrawerToggle.syncState();
            return true;
        }
        // Handle action buttons
        switch (item.getItemId()) {
        case android.R.id.home:
            if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
                mDrawerLayout.closeDrawer(mDrawerRight);
            }
            mDrawerToggle.syncState();
            return true;
        case R.id.action_back:
            if (mCurrentView != null) {
                if (mCurrentView.canGoBack()) {
                    mCurrentView.goBack();
                }
            }
            return true;
        case R.id.action_forward:
            if (mCurrentView != null) {
                if (mCurrentView.canGoForward()) {
                    mCurrentView.goForward();
                }
            }
            return true;
        case R.id.action_new_tab:
            newTab(null, true);
            return true;
            // case R.id.action_incognito:
            // startActivity(new Intent(this, IncognitoActivity.class));
            // return true;
        case R.id.action_share:
            if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
                Intent shareIntent = new Intent(
                        android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        mCurrentView.getTitle());
                String shareMessage = mCurrentView.getUrl();
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                        shareMessage);
                startActivity(Intent.createChooser(shareIntent, getResources()
                        .getString(R.string.dialog_title_share)));
            }
            return true;
        case R.id.action_bookmarks:
            openBookmarks();
            return true;
        case R.id.action_copy:
            if (mCurrentView != null) {
                if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("label", mCurrentView
                            .getUrl().toString());
                    clipboard.setPrimaryClip(clip);
                    Utils.showToast(mContext, mContext.getResources()
                            .getString(R.string.message_link_copied));
                }
            }
            return true;
        case R.id.action_user_agent:
        case R.id.action_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.action_history:
            openHistory();
            return true;
        case R.id.action_add_bookmark:
            if (!mCurrentView.getUrl().startsWith(Constants.FILE)) {
                HistoryItem bookmark = new HistoryItem(mCurrentView.getUrl(),
                        mCurrentView.getTitle());
                if (mBookmarkManager.addBookmark(bookmark)) {
                    mBookmarkList.add(bookmark);
                    Collections.sort(mBookmarkList, new SortIgnoreCase());
                    notifyBookmarkDataSetChanged();
                    mSearchAdapter.refreshBookmarks();
                }
            }
            return true;
        case R.id.action_find:
            findInPage();
            return true;

        case R.id.action_fling:
            if (mMediaFlingBar.getVisibility() != View.GONE) {
                mMediaFlingBar.hide();
            } else {
                mMediaFlingBar.show();
            }

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * refreshes the underlying list of the Bookmark adapter since the bookmark
     * adapter doesn't always change when notifyDataChanged gets called.
     */
    private void notifyBookmarkDataSetChanged() {
        mBookmarkAdapter.clear();
        mBookmarkAdapter.addAll(mBookmarkList);
        mBookmarkAdapter.notifyDataSetChanged();
    }

    /**
     * method that shows a dialog asking what string the user wishes to search
     * for. It highlights the text entered.
     */
    private void findInPage() {
        final AlertDialog.Builder finder = new AlertDialog.Builder(mActivity);
        finder.setTitle(getResources().getString(R.string.action_find));
        final EditText getHome = new EditText(this);
        getHome.setHint(getResources().getString(R.string.search_hint));
        finder.setView(getHome);
        finder.setPositiveButton(
                getResources().getString(R.string.search_hint),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String query = getHome.getText().toString();
                        if (query.length() > 0)
                            showSearchInterfaceBar(query);
                    }
                });
        finder.show();
    }

    private void showSearchInterfaceBar(String text) {
        if (mCurrentView != null) {
            mCurrentView.find(text);
        }

        final RelativeLayout bar = (RelativeLayout) findViewById(R.id.search_bar);
        bar.setVisibility(View.VISIBLE);

        TextView tw = (TextView) findViewById(R.id.search_query);
        tw.setText("'" + text + "'");

        ImageButton up = (ImageButton) findViewById(R.id.button_next);
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // mCurrentView.getWebView().findNext(false);
            }
        });
        ImageButton down = (ImageButton) findViewById(R.id.button_back);
        down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // mCurrentView.getWebView().findNext(true);
            }
        });

        ImageButton quit = (ImageButton) findViewById(R.id.button_quit);
        quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // mCurrentView.getWebView().clearMatches();
                bar.setVisibility(View.GONE);
            }
        });
    }

    /**
     * The click listener for ListView in the navigation drawer
     */
    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            mIsNewIntent = false;
            selectItem(position);
        }
    }

    /**
     * long click listener for Navigation Drawer
     */
    private class DrawerItemLongClickListener implements
            ListView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                int position, long arg3) {
            deleteTab(position);
            return false;
        }
    }

    private class BookmarkItemClickListener implements
            ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            if (mCurrentView != null) {
                mCurrentView.loadUrl(mBookmarkList.get(position).getUrl());
            }
            // keep any jank from happening when the drawer is closed after the
            // URL starts to load
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.closeDrawer(mDrawerRight);
                }
            }, 150);
        }
    }

    private class BookmarkItemLongClickListener implements
            ListView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                final int position, long arg3) {

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle(mContext.getResources().getString(
                    R.string.action_bookmarks));
            builder.setMessage(
                    getResources().getString(R.string.dialog_bookmark))
                    .setCancelable(true)
                    .setPositiveButton(
                            getResources().getString(R.string.action_new_tab),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    newTab(mBookmarkList.get(position).getUrl(),
                                            false);
                                    mDrawerLayout.closeDrawers();
                                }
                            })
                    .setNegativeButton(
                            getResources().getString(R.string.action_delete),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    if (mBookmarkManager
                                            .deleteBookmark(mBookmarkList.get(
                                                    position).getUrl())) {
                                        mBookmarkList.remove(position);
                                        notifyBookmarkDataSetChanged();
                                        mSearchAdapter.refreshBookmarks();
                                        openBookmarks();
                                    }
                                }
                            })
                    .setNeutralButton(
                            getResources().getString(R.string.action_edit),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    editBookmark(position);
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        }
    }

    /**
     * Takes in the id of which bookmark was selected and shows a dialog that
     * allows the user to rename and change the url of the bookmark
     * 
     * @param id
     *            which id in the list was chosen
     */
    public synchronized void editBookmark(final int id) {
        final AlertDialog.Builder homePicker = new AlertDialog.Builder(
                mActivity);
        homePicker.setTitle(getResources().getString(
                R.string.title_edit_bookmark));
        final EditText getTitle = new EditText(mContext);
        getTitle.setHint(getResources().getString(R.string.hint_title));
        getTitle.setText(mBookmarkList.get(id).getTitle());
        getTitle.setSingleLine();
        final EditText getUrl = new EditText(mContext);
        getUrl.setHint(getResources().getString(R.string.hint_url));
        getUrl.setText(mBookmarkList.get(id).getUrl());
        getUrl.setSingleLine();
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(getTitle);
        layout.addView(getUrl);
        homePicker.setView(layout);
        homePicker.setPositiveButton(
                getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBookmarkList.get(id).setTitle(
                                getTitle.getText().toString());
                        mBookmarkList.get(id).setUrl(
                                getUrl.getText().toString());
                        mBookmarkManager.overwriteBookmarks(mBookmarkList);
                        Collections.sort(mBookmarkList, new SortIgnoreCase());
                        notifyBookmarkDataSetChanged();
                        if (mCurrentView != null) {
                            if (mCurrentView.getUrl()
                                    .startsWith(Constants.FILE)
                                    && mCurrentView.getUrl().endsWith(
                                            "bookmarks.html")) {
                                // openBookmarkPage(mCurrentView.getWebView());
                            }
                        }
                    }
                });
        homePicker.show();
    }

    /**
     * displays the WebView contained in the LightningView Also handles the
     * removal of previous views
     * 
     * @param view
     *            the LightningView to show
     */
    private synchronized void showTab(LightningView view) {
        if (view == null) {
            return;
        }
        mBrowserFrame.removeAllViews();
        if (mCurrentView != null) {
            mCurrentView.setForegroundTab(false);
            mCurrentView.onPause();
        }
        mCurrentView = view;
        mCurrentView.setForegroundTab(true);
        if (mCurrentView.getWebView() != null) {
            updateUrl(mCurrentView.getUrl());
            updateProgress(mCurrentView.getProgress());
        } else {
            updateUrl("");
            updateProgress(0);
        }

        mBrowserFrame.addView(mCurrentView.getWebView(), mMatchParent);
        mCurrentView.onResume();

        // Use a delayed handler to make the transition smooth
        // otherwise it will get caught up with the showTab code
        // and cause a janky motion
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.closeDrawers();
            }
        }, 150);
    }

    /**
     * creates a new tab with the passed in URL if it isn't null
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    public void handleNewIntent(Intent intent) {
        if (mCurrentView == null) {
            initialize();
        }

        String url = null;
        if (intent != null) {
            url = intent.getDataString();
        }
        int num = 0;
        if (intent != null && intent.getExtras() != null) {
            num = intent.getExtras().getInt(getPackageName() + ".Origin");
        }
        if (num == 1) {
            mCurrentView.loadUrl(url);
        } else if (url != null) {
            if (url.startsWith(Constants.FILE)) {
                Utils.showToast(this,
                        getResources()
                                .getString(R.string.message_blocked_local));
                url = null;
            }
            newTab(url, true);
            mIsNewIntent = true;
        }
    }

    @Override
    public void closeEmptyTab() {
        // if (mCurrentView != null &&
        // mCurrentView.getWebView().copyBackForwardList().getSize() == 0) {
        if (mCurrentView != null
                && mCurrentView.getWebView().getNavigationHistory().size() == 0) {
            closeCurrentTab();
        }
    }

    private void closeCurrentTab() {
        // don't delete the tab because the browser will close and mess stuff up
    }

    private void selectItem(final int position) {
        // update selected item and title, then close the drawer

        showTab(mWebViews.get(position));

    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    protected synchronized void newTab(String url, boolean show) {
        mIsNewIntent = false;
        LightningView startingTab = new LightningView(mActivity, url);
        if (mIdGenerator == 0) {
            startingTab.resumeTimers();
        }
        mIdGenerator++;
        mWebViews.add(startingTab);

        Drawable icon = writeOnDrawable(mWebViews.size());
        mActionBar.setIcon(icon);
        mTitleAdapter.notifyDataSetChanged();
        if (show) {
            mDrawerListLeft.setItemChecked(mWebViews.size() - 1, true);
            showTab(startingTab);
        }
    }

    private synchronized void deleteTab(int position) {
        if (position >= mWebViews.size()) {
            return;
        }

        int current = mDrawerListLeft.getCheckedItemPosition();
        LightningView reference = mWebViews.get(position);
        if (reference == null) {
            return;
        }
        if (reference.getUrl() != null
                && !reference.getUrl().startsWith(Constants.FILE)) {
            mEditPrefs.putString(PreferenceConstants.SAVE_URL,
                    reference.getUrl()).apply();
        }
        boolean isShown = reference.isShown();
        if (current > position) {
            mWebViews.remove(position);
            mDrawerListLeft.setItemChecked(current - 1, true);
            reference.onDestroy();
        } else if (mWebViews.size() > position + 1) {
            if (current == position) {
                showTab(mWebViews.get(position + 1));
                mWebViews.remove(position);
                mDrawerListLeft.setItemChecked(position, true);
            } else {
                mWebViews.remove(position);
            }

            reference.onDestroy();
        } else if (mWebViews.size() > 1) {
            if (current == position) {
                showTab(mWebViews.get(position - 1));
                mWebViews.remove(position);
                mDrawerListLeft.setItemChecked(position - 1, true);
            } else {
                mWebViews.remove(position);
            }

            reference.onDestroy();
        } else {
            if (mCurrentView.getUrl() == null
                    || mCurrentView.getUrl().startsWith(Constants.FILE)
                    || mCurrentView.getUrl().equals(mHomepage)) {
                closeActivity();
            } else {
                mWebViews.remove(position);
                if (mPreferences.getBoolean(
                        PreferenceConstants.CLEAR_CACHE_EXIT, false)
                        && mCurrentView != null && !isIncognito()) {
                    mCurrentView.clearCache(true);
                    Log.i(Constants.TAG, "Cache Cleared");

                }
                if (mPreferences.getBoolean(
                        PreferenceConstants.CLEAR_HISTORY_EXIT, false)
                        && !isIncognito()) {
                    clearHistory();
                    Log.i(Constants.TAG, "History Cleared");

                }
                if (mPreferences.getBoolean(
                        PreferenceConstants.CLEAR_COOKIES_EXIT, false)
                        && !isIncognito()) {
                    clearCookies();
                    Log.i(Constants.TAG, "Cookies Cleared");

                }
                if (reference != null) {
                    reference.pauseTimers();
                    reference.onDestroy();
                }
                mCurrentView = null;
                mTitleAdapter.notifyDataSetChanged();
                closeActivity();

            }
        }
        mTitleAdapter.notifyDataSetChanged();
        Drawable icon = writeOnDrawable(mWebViews.size());
        mActionBar.setIcon(icon);

        if (mIsNewIntent && isShown) {
            mIsNewIntent = false;
            closeActivity();
        }

        Log.i(Constants.TAG, "deleted tab");
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mPreferences.getBoolean(PreferenceConstants.CLEAR_CACHE_EXIT,
                    false) && mCurrentView != null && !isIncognito()) {
                mCurrentView.clearCache(true);
                Log.i(Constants.TAG, "Cache Cleared");

            }
            if (mPreferences.getBoolean(PreferenceConstants.CLEAR_HISTORY_EXIT,
                    false) && !isIncognito()) {
                clearHistory();
                Log.i(Constants.TAG, "History Cleared");

            }
            if (mPreferences.getBoolean(PreferenceConstants.CLEAR_COOKIES_EXIT,
                    false) && !isIncognito()) {
                clearCookies();
                Log.i(Constants.TAG, "Cookies Cleared");

            }
            mCurrentView = null;
            for (int n = 0; n < mWebViews.size(); n++) {
                if (mWebViews.get(n) != null) {
                    mWebViews.get(n).onDestroy();
                }
            }
            mWebViews.clear();
            mTitleAdapter.notifyDataSetChanged();
            finish();
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    public void clearHistory() {
        this.deleteDatabase(HistoryDatabaseHandler.DATABASE_NAME);
        WebViewDatabase m = WebViewDatabase.getInstance(this);
        m.clearFormData();
        m.clearHttpAuthUsernamePassword();
        if (API < 18) {
            m.clearUsernamePassword();
            WebIconDatabase.getInstance().removeAllIcons();
        }
        if (mSystemBrowser) {
            try {
                Browser.clearHistory(getContentResolver());
            } catch (NullPointerException ignored) {
            }
        }
        Utils.trimCache(this);
    }

    public void clearCookies() {
        CookieManager c = CookieManager.getInstance();
        CookieSyncManager.createInstance(this);
        c.removeAllCookie();
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed!");
        if (!mActionBar.isShowing()) {
            mActionBar.show();
        }
        if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
            mDrawerLayout.closeDrawer(mDrawerLeft);
        } else if (mDrawerLayout.isDrawerOpen(mDrawerRight)) {
            mDrawerLayout.closeDrawer(mDrawerRight);
        } else {
            if (mCurrentView != null) {
                Log.e(TAG, "onBackPressed");
                if (mCurrentView.canGoBack()) {
                    if (!mCurrentView.isShown()) {
                        onHideCustomView();
                    } else {
                        Log.e(TAG, "goBack!");
                        mCurrentView.goBack();
                    }
                } else {
                    Log.e(TAG, "deleteTab!");
                    deleteTab(mDrawerListLeft.getCheckedItemPosition());
                }
            } else {
                Log.e(TAG, "So madness. Much confusion. Why happen.");
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(Constants.TAG, "onPause");
        if (mCurrentView != null) {
            mCurrentView.pauseTimers();
            mCurrentView.onPause();
        }
        if (mHistoryHandler != null) {
            if (mHistoryHandler.isOpen()) {
                mHistoryHandler.close();
            }
        }

    }

    public void saveOpenTabs() {
        if (mPreferences
                .getBoolean(PreferenceConstants.RESTORE_LOST_TABS, true)) {
            String s = "";
            for (int n = 0; n < mWebViews.size(); n++) {
                if (mWebViews.get(n).getUrl() != null) {
                    s = s + mWebViews.get(n).getUrl() + "|$|SEPARATOR|$|";
                }
            }
            mEditPrefs.putString(PreferenceConstants.URL_MEMORY, s);
            mEditPrefs.commit();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(Constants.TAG, "onDestroy");
        if (mHistoryHandler != null) {
            if (mHistoryHandler.isOpen()) {
                mHistoryHandler.close();
            }
        }

        mQuit = true;

        if (mServerSocket != null) {
            try {
                mServerSocket.close();
                mServerSocket = null;
            } catch (Exception e) {
            }
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(Constants.TAG, "onResume");
        if (mSearchAdapter != null) {
            mSearchAdapter.refreshPreferences();
            mSearchAdapter.refreshBookmarks();
        }
        if (mActionBar != null) {
            if (!mActionBar.isShowing()) {
                mActionBar.show();
            }
        }
        if (mCurrentView != null) {
            mCurrentView.resumeTimers();
            mCurrentView.onResume();

            if (mHistoryHandler == null) {
                mHistoryHandler = new HistoryDatabaseHandler(this);
            } else if (!mHistoryHandler.isOpen()) {
                mHistoryHandler = new HistoryDatabaseHandler(this);
            }
            mBookmarkList = mBookmarkManager.getBookmarks(true);
            notifyBookmarkDataSetChanged();
        } else {
            initialize();
        }
        initializePreferences();
        if (mWebViews != null) {
            for (int n = 0; n < mWebViews.size(); n++) {
                if (mWebViews.get(n) != null) {
                    mWebViews.get(n).initializePreferences(this);
                } else {
                    mWebViews.remove(n);
                }
            }
        } else {
            initialize();
        }
    }

    /**
     * searches the web for the query fixing any and all problems with the input
     * checks if it is a search, url, etc.
     */
    void searchTheWeb(String query) {
        if (query.equals("")) {
            return;
        }
        String SEARCH = mSearchText;
        query = query.trim();
        mCurrentView.stopLoading();

        if (query.startsWith("www.")) {
            query = Constants.HTTP + query;
        } else if (query.startsWith("ftp.")) {
            query = "ftp://" + query;
        }

        boolean containsPeriod = query.contains(".");
        boolean isIPAddress = (TextUtils.isDigitsOnly(query.replace(".", ""))
                && (query.replace(".", "").length() >= 4) && query
                .contains("."));
        boolean aboutScheme = query.contains("about:");
        boolean validURL = (query.startsWith("ftp://")
                || query.startsWith(Constants.HTTP)
                || query.startsWith(Constants.FILE) || query
                    .startsWith(Constants.HTTPS)) || isIPAddress;
        boolean isSearch = ((query.contains(" ") || !containsPeriod) && !aboutScheme);

        if (isIPAddress
                && (!query.startsWith(Constants.HTTP) || !query
                        .startsWith(Constants.HTTPS))) {
            query = Constants.HTTP + query;
        }

        if (isSearch) {
            try {
                query = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            mCurrentView.loadUrl(SEARCH + query);
        } else if (!validURL) {
            mCurrentView.loadUrl(Constants.HTTP + query);
        } else {
            mCurrentView.loadUrl(query);
        }
    }

    private int pixelsToDp(int num) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) ((num - 0.5f) / scale);
    }

    /**
     * writes the number of open tabs on the icon.
     */
    public BitmapDrawable writeOnDrawable(int number) {

        Bitmap bm = Bitmap.createBitmap(mActionBarSize, mActionBarSize,
                Config.ARGB_8888);
        String text = number + "";
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setStyle(Style.FILL);
        paint.setColor(mNumberIconColor);
        if (number > 99) {
            number = 99;
        }
        // pixels, 36 dp
        if (mActionBarSizeDp < 50) {
            if (number > 9) {
                paint.setTextSize(mActionBarSize * 3 / 4); // originally
                // 40
                // pixels,
                // 24 dp
            } else {
                paint.setTextSize(mActionBarSize * 9 / 10); // originally 50
                // pixels, 30 dp
            }
        } else {
            paint.setTextSize(mActionBarSize * 3 / 4);
        }
        Canvas canvas = new Canvas(bm);
        // originally only vertical padding of 5 pixels

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint
                .ascent()) / 2));

        canvas.drawText(text, xPos, yPos, paint);

        return new BitmapDrawable(getResources(), bm);
    }

    public class LightningViewAdapter extends ArrayAdapter<LightningView> {

        Context context;

        int layoutResourceId;

        List<LightningView> data = null;

        public LightningViewAdapter(Context context, int layoutResourceId,
                List<LightningView> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(final int position, View convertView,
                ViewGroup parent) {
            View row = convertView;
            LightningViewHolder holder = null;
            if (row == null) {
                LayoutInflater inflater = ((Activity) context)
                        .getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new LightningViewHolder();
                holder.txtTitle = (TextView) row.findViewById(R.id.text1);
                holder.favicon = (ImageView) row.findViewById(R.id.favicon1);
                holder.exit = (ImageView) row.findViewById(R.id.delete1);
                holder.exit.setTag(position);
                row.setTag(holder);
            } else {
                holder = (LightningViewHolder) row.getTag();
            }

            holder.exit.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View view) {
                    deleteTab(position);
                }

            });

            LightningView web = data.get(position);
            holder.txtTitle.setText(web.getTitle());
            if (web.isForegroundTab()) {
                holder.txtTitle.setTextAppearance(context, R.style.boldText);
            } else {
                holder.txtTitle.setTextAppearance(context, R.style.normalText);
            }

            Bitmap favicon = web.getFavicon();
            if (web.isForegroundTab()) {

                holder.favicon.setImageBitmap(favicon);
            } else {
                Bitmap grayscaleBitmap = Bitmap.createBitmap(
                        favicon.getWidth(), favicon.getHeight(),
                        Bitmap.Config.ARGB_8888);

                Canvas c = new Canvas(grayscaleBitmap);
                Paint p = new Paint();
                ColorMatrix cm = new ColorMatrix();

                cm.setSaturation(0);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(cm);
                p.setColorFilter(filter);
                c.drawBitmap(favicon, 0, 0, p);
                holder.favicon.setImageBitmap(grayscaleBitmap);
            }
            return row;
        }

        class LightningViewHolder {

            TextView txtTitle;

            ImageView favicon;

            ImageView exit;
        }
    }

    public class BookmarkViewAdapter extends ArrayAdapter<HistoryItem> {

        Context context;

        int layoutResourceId;

        List<HistoryItem> data = null;

        public BookmarkViewAdapter(Context context, int layoutResourceId,
                List<HistoryItem> data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            BookmarkViewHolder holder = null;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context)
                        .getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new BookmarkViewHolder();
                holder.txtTitle = (TextView) row.findViewById(R.id.text1);
                holder.favicon = (ImageView) row.findViewById(R.id.favicon1);
                row.setTag(holder);
            } else {
                holder = (BookmarkViewHolder) row.getTag();
            }

            HistoryItem web = data.get(position);
            holder.txtTitle.setText(web.getTitle());
            holder.favicon.setImageBitmap(mWebpageBitmap);
            if (web.getBitmap() == null) {
                getImage(holder.favicon, web);
            } else {
                holder.favicon.setImageBitmap(web.getBitmap());
            }
            return row;
        }

        class BookmarkViewHolder {

            TextView txtTitle;

            ImageView favicon;
        }
    }

    public void getImage(ImageView image, HistoryItem web) {
        try {
            new DownloadImageTask(image, web).execute(Constants.HTTP
                    + getDomainName(web.getUrl()) + "/favicon.ico");
        } catch (URISyntaxException e) {
            new DownloadImageTask(image, web)
                    .execute("https://www.google.com/s2/favicons?domain_url="
                            + web.getUrl());
            e.printStackTrace();
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        ImageView bmImage;

        HistoryItem mWeb;

        public DownloadImageTask(ImageView bmImage, HistoryItem web) {
            this.bmImage = bmImage;
            this.mWeb = web;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon = null;
            // unique path for each url that is bookmarked.
            String hash = String.valueOf(urldisplay.hashCode());
            File image = new File(mContext.getCacheDir(), hash + ".png");
            // checks to see if the image exists
            if (!image.exists()) {
                try {
                    // if not, download it...
                    URL url = new URL(urldisplay);
                    HttpURLConnection connection = (HttpURLConnection) url
                            .openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream in = connection.getInputStream();

                    if (in != null) {
                        mIcon = BitmapFactory.decodeStream(in);
                    }
                    // ...and cache it
                    if (mIcon != null) {
                        FileOutputStream fos = new FileOutputStream(image);
                        mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();
                        Log.i(Constants.TAG, "Downloaded: " + urldisplay);
                    }

                } catch (Exception e) {
                } finally {

                }
            } else {
                // if it exists, retrieve it from the cache
                mIcon = BitmapFactory.decodeFile(image.getPath());
            }
            if (mIcon == null) {
                try {
                    // if not, download it...
                    InputStream in = new java.net.URL(
                            "https://www.google.com/s2/favicons?domain_url="
                                    + urldisplay).openStream();

                    if (in != null) {
                        mIcon = BitmapFactory.decodeStream(in);
                    }
                    // ...and cache it
                    if (mIcon != null) {
                        FileOutputStream fos = new FileOutputStream(image);
                        mIcon.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();
                    }

                } catch (Exception e) {
                }
            }
            if (mIcon == null) {
                return mWebpageBitmap;
            } else {
                return mIcon;
            }
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
            mWeb.setBitmap(result);
            notifyBookmarkDataSetChanged();
        }
    }

    static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain == null) {
            return url;
        }
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    @Override
    public void updateUrl(String url) {
        if (url == null) {
            return;
        }
        url = url.replaceFirst(Constants.HTTP, "");
        if (url.startsWith(Constants.FILE)) {
            url = "";
        }

        mSearch.setText(url);
    }

    @Override
    public void updateProgress(int n) {
        if (n > mProgressBar.getProgress()) {
            ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar,
                    "progress", n);
            animator.setDuration(200);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.start();
        } else if (n < mProgressBar.getProgress()) {
            ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar,
                    "progress", 0, n);
            animator.setDuration(200);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.start();
        }
        if (n >= 100) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    setIsFinishedLoading();
                }
            }, 200);

        } else {
            mProgressBar.setVisibility(View.VISIBLE);
            setIsLoading();
        }
    }

    @Override
    public void updateHistory(final String title, final String url) {

    }

    public void addItemToHistory(final String title, final String url) {
        Runnable update = new Runnable() {
            @Override
            public void run() {
                if (isSystemBrowserAvailable()
                        && mPreferences.getBoolean(
                                PreferenceConstants.SYNC_HISTORY, true)) {
                    try {
                        Browser.updateVisitedHistory(getContentResolver(), url,
                                true);
                    } catch (NullPointerException ignored) {
                    }
                }
                try {
                    if (mHistoryHandler == null && !mHistoryHandler.isOpen()) {
                        mHistoryHandler = new HistoryDatabaseHandler(mContext);
                    }
                    mHistoryHandler.visitHistoryItem(url, title);
                } catch (IllegalStateException e) {
                    Log.e(Constants.TAG,
                            "IllegalStateException in updateHistory");
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG,
                            "NullPointerException in updateHistory");
                } catch (SQLiteException e) {
                    Log.e(Constants.TAG, "SQLiteException in updateHistory");
                }
            }
        };
        if (url != null && !url.startsWith(Constants.FILE)) {
            new Thread(update).start();
        }
    }

    public boolean isSystemBrowserAvailable() {
        return mSystemBrowser;
    }

    public boolean getSystemBrowser() {
        Cursor c = null;
        String[] columns = new String[] { "url", "title" };
        boolean browserFlag = false;
        try {

            Uri bookmarks = Browser.BOOKMARKS_URI;
            c = getContentResolver()
                    .query(bookmarks, columns, null, null, null);
        } catch (SQLiteException ignored) {
        } catch (IllegalStateException ignored) {
        } catch (NullPointerException ignored) {
        }

        if (c != null) {
            Log.i("Browser", "System Browser Available");
            browserFlag = true;
        } else {
            Log.e("Browser", "System Browser Unavailable");
            browserFlag = false;
        }
        if (c != null) {
            c.close();
            c = null;
        }
        mEditPrefs.putBoolean("SystemBrowser", browserFlag);
        mEditPrefs.commit();
        return browserFlag;
    }

    /**
     * method to generate search suggestions for the AutoCompleteTextView from
     * previously searched URLs
     */
    private void initializeSearchSuggestions(final AutoCompleteTextView getUrl) {

        getUrl.setThreshold(1);
        getUrl.setDropDownWidth(-1);
        getUrl.setDropDownAnchor(R.id.progressWrapper);
        getUrl.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                try {
                    String url;
                    url = ((TextView) arg1.findViewById(R.id.url)).getText()
                            .toString();
                    if (url.startsWith(mContext.getString(R.string.suggestion))) {
                        url = ((TextView) arg1.findViewById(R.id.title))
                                .getText().toString();
                    } else {
                        getUrl.setText(url);
                    }
                    searchTheWeb(url);
                    url = null;
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getUrl.getWindowToken(), 0);
                    if (mCurrentView != null) {
                        mCurrentView.requestFocus();
                    }
                } catch (NullPointerException e) {
                    Log.e("Browser Error: ",
                            "NullPointerException on item click");
                }
            }

        });

        getUrl.setSelectAllOnFocus(true);
        mSearchAdapter = new SearchAdapter(mContext, isIncognito());
        getUrl.setAdapter(mSearchAdapter);
    }

    @Override
    public boolean isIncognito() {
        return false;
    }

    /**
     * function that opens the HTML history page in the browser
     */
    private void openHistory() {
        // use a thread so that history retrieval doesn't block the UI
        Thread history = new Thread(new Runnable() {

            @Override
            public void run() {
                mCurrentView.loadUrl(HistoryPage.getHistoryPage(mContext));
                mSearch.setText("");
            }

        });
        history.run();
    }

    /**
     * helper function that opens the bookmark drawer
     */
    private void openBookmarks() {
        if (mDrawerLayout.isDrawerOpen(mDrawerLeft)) {
            mDrawerLayout.closeDrawers();
        }
        mDrawerToggle.syncState();
        mDrawerLayout.openDrawer(mDrawerRight);
    }

    public void closeDrawers() {
        mDrawerLayout.closeDrawers();
    }

    @Override
    /**
     * open the HTML bookmarks page, parameter view is the WebView that should show the page
     */
    public void openBookmarkPage(WebView view) {
        String bookmarkHtml = BookmarkPage.HEADING;
        Iterator<HistoryItem> iter = mBookmarkList.iterator();
        HistoryItem helper;
        while (iter.hasNext()) {
            helper = iter.next();
            bookmarkHtml += (BookmarkPage.PART1 + helper.getUrl()
                    + BookmarkPage.PART2 + helper.getUrl() + BookmarkPage.PART3
                    + helper.getTitle() + BookmarkPage.PART4);
        }
        bookmarkHtml += BookmarkPage.END;
        File bookmarkWebPage = new File(mContext.getFilesDir(),
                BookmarkPage.FILENAME);
        try {
            FileWriter bookWriter = new FileWriter(bookmarkWebPage, false);
            bookWriter.write(bookmarkHtml);
            bookWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        view.loadUrl(Constants.FILE + bookmarkWebPage);
    }

    @Override
    public void update() {
        mTitleAdapter.notifyDataSetChanged();
    }

    @Override
    /**
     * opens a file chooser
     * param ValueCallback is the message from the WebView indicating a file chooser
     * should be opened
     */
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        mUploadMessage = uploadMsg;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(
                Intent.createChooser(i, getString(R.string.title_file_chooser)),
                1);
    }

    @Override
    /**
     * used to allow uploading into the browser
     */
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        if (requestCode == 1) {
            if (null == mUploadMessage) {
                return;
            }
            Uri result = intent == null || resultCode != RESULT_OK ? null
                    : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;

        }
    }

    @Override
    /**
     * handles long presses for the browser, tries to get the
     * url of the item that was clicked and sends it (it can be null)
     * to the click handler that does cool stuff with it
     */
    public void onLongPress() {
        if (mClickHandler == null) {
            mClickHandler = new ClickHandler(mContext);
        }
        Message click = mClickHandler.obtainMessage();
        if (click != null) {
            click.setTarget(mClickHandler);
        }
        // mCurrentView.getWebView().requestFocusNodeHref(click);
    }

    @Override
    public void onShowCustomView(View view, int requestedOrientation,
            CustomViewCallback callback) {
        if (view == null) {
            return;
        }
        if (mCustomView != null && callback != null) {
            callback.onCustomViewHidden();
            return;
        }
        view.setKeepScreenOn(true);
        mOriginalOrientation = getRequestedOrientation();
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        mFullscreenContainer = new FullscreenHolder(this);
        mCustomView = view;
        mFullscreenContainer.addView(mCustomView, COVER_SCREEN_PARAMS);
        decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
        setFullscreen(true);
        mCurrentView.setVisibility(View.GONE);
        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                mVideoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                mVideoView.setOnErrorListener(new VideoCompletionListener());
                mVideoView
                        .setOnCompletionListener(new VideoCompletionListener());
            }
        }
        mCustomViewCallback = callback;
    }

    @Override
    public void onHideCustomView() {
        if (mCustomView == null || mCustomViewCallback == null
                || mCurrentView == null) {
            return;
        }
        Log.i(Constants.TAG, "onHideCustomView");
        mCurrentView.setVisibility(View.VISIBLE);
        mCustomView.setKeepScreenOn(false);
        setFullscreen(mPreferences.getBoolean(
                PreferenceConstants.HIDE_STATUS_BAR, false));
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        if (decor != null) {
            decor.removeView(mFullscreenContainer);
        }

        if (API < 19) {
            try {
                mCustomViewCallback.onCustomViewHidden();
            } catch (Throwable ignored) {

            }
        }
        mFullscreenContainer = null;
        mCustomView = null;
        if (mVideoView != null) {
            mVideoView.setOnErrorListener(null);
            mVideoView.setOnCompletionListener(null);
            mVideoView = null;
        }
        setRequestedOrientation(mOriginalOrientation);
    }

    private class VideoCompletionListener implements
            MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            onHideCustomView();
        }

    }

    /**
     * turns on fullscreen mode in the app
     * 
     * @param enabled
     *            whether to enable fullscreen or not
     */
    public void setFullscreen(boolean enabled) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (enabled) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
            if (mCustomView != null) {
                mCustomView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            } else {
                mBrowserFrame
                        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
        win.setAttributes(winParams);
    }

    /**
     * a class extending FramLayout used to display fullscreen videos
     */
    static class FullscreenHolder extends FrameLayout {

        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ctx.getResources().getColor(
                    android.R.color.black));
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }

    }

    @Override
    /**
     * a stupid method that returns the bitmap image to display in place of
     * a loading video
     */
    public Bitmap getDefaultVideoPoster() {
        if (mDefaultVideoPoster == null) {
            mDefaultVideoPoster = BitmapFactory.decodeResource(getResources(),
                    android.R.drawable.ic_media_play);
        }
        return mDefaultVideoPoster;
    }

    @SuppressLint("InflateParams")
    @Override
    /**
     * dumb method that returns the loading progress for a video
     */
    public View getVideoLoadingProgressView() {
        if (mVideoProgressView == null) {
            LayoutInflater inflater = LayoutInflater.from(this);
            mVideoProgressView = inflater.inflate(
                    R.layout.video_loading_progress, null);
        }
        return mVideoProgressView;
    }

    @Override
    /**
     * handles javascript requests to create a new window in the browser
     */
    public void onCreateWindow(boolean isUserGesture, Message resultMsg) {
        if (resultMsg == null) {
            return;
        }
        newTab("", true);
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        // transport.setWebView(mCurrentView.getWebView());
        resultMsg.sendToTarget();
    }

    @Override
    /**
     * returns the Activity instance for this activity,
     * very helpful when creating things in other classes... I think
     */
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * it hides the action bar, seriously what else were you expecting
     */
    @Override
    public void hideActionBar() {
        if (mActionBar.isShowing() && mFullScreen) {
            mActionBar.hide();
        }
    }

    @Override
    /**
     * obviously it shows the action bar if it's hidden
     */
    public void showActionBar() {
        if (!mActionBar.isShowing() && mFullScreen) {
            mActionBar.show();
        }
    }

    @Override
    /**
     * handles a long click on the page, parameter String url 
     * is the url that should have been obtained from the WebView touch node
     * thingy, if it is null, this method tries to deal with it and find a workaround
     */
    public void longClickPage(final String url) {
        // HitTestResult result = null;
        // if (mCurrentView.getWebView() != null) {
        // result = mCurrentView.getWebView().getHitTestResult();
        // }
        // if (url != null) {
        // if (result != null) {
        // if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        // || result.getType() == HitTestResult.IMAGE_TYPE) {
        // DialogInterface.OnClickListener dialogClickListener = new
        // DialogInterface.OnClickListener() {
        // @Override
        // public void onClick(DialogInterface dialog, int which) {
        // switch (which) {
        // case DialogInterface.BUTTON_POSITIVE:
        // newTab(url, false);
        // break;
        //
        // case DialogInterface.BUTTON_NEGATIVE:
        // mCurrentView.loadUrl(url);
        // break;
        //
        // case DialogInterface.BUTTON_NEUTRAL:
        // if (API > 8) {
        // Utils.downloadFile(mActivity, url,
        // mCurrentView.getUserAgent(), "attachment", false);
        // }
        // break;
        // }
        // }
        // };
        //
        // AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); //
        // dialog
        // builder.setTitle(url.replace(Constants.HTTP, ""))
        // .setMessage(getResources().getString(R.string.dialog_image))
        // .setPositiveButton(getResources().getString(R.string.action_new_tab),
        // dialogClickListener)
        // .setNegativeButton(getResources().getString(R.string.action_open),
        // dialogClickListener)
        // .setNeutralButton(getResources().getString(R.string.action_download),
        // dialogClickListener).show();
        //
        // } else {
        // DialogInterface.OnClickListener dialogClickListener = new
        // DialogInterface.OnClickListener() {
        // @Override
        // public void onClick(DialogInterface dialog, int which) {
        // switch (which) {
        // case DialogInterface.BUTTON_POSITIVE:
        // newTab(url, false);
        // break;
        //
        // case DialogInterface.BUTTON_NEGATIVE:
        // mCurrentView.loadUrl(url);
        // break;
        //
        // case DialogInterface.BUTTON_NEUTRAL:
        // ClipboardManager clipboard = (ClipboardManager)
        // getSystemService(CLIPBOARD_SERVICE);
        // ClipData clip = ClipData.newPlainText("label", url);
        // clipboard.setPrimaryClip(clip);
        // break;
        // }
        // }
        // };
        //
        // AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); //
        // dialog
        // builder.setTitle(url)
        // .setMessage(getResources().getString(R.string.dialog_link))
        // .setPositiveButton(getResources().getString(R.string.action_new_tab),
        // dialogClickListener)
        // .setNegativeButton(getResources().getString(R.string.action_open),
        // dialogClickListener)
        // .setNeutralButton(getResources().getString(R.string.action_copy),
        // dialogClickListener).show();
        // }
        // } else {
        // DialogInterface.OnClickListener dialogClickListener = new
        // DialogInterface.OnClickListener() {
        // @Override
        // public void onClick(DialogInterface dialog, int which) {
        // switch (which) {
        // case DialogInterface.BUTTON_POSITIVE:
        // newTab(url, false);
        // break;
        //
        // case DialogInterface.BUTTON_NEGATIVE:
        // mCurrentView.loadUrl(url);
        // break;
        //
        // case DialogInterface.BUTTON_NEUTRAL:
        // ClipboardManager clipboard = (ClipboardManager)
        // getSystemService(CLIPBOARD_SERVICE);
        // ClipData clip = ClipData.newPlainText("label", url);
        // clipboard.setPrimaryClip(clip);
        //
        // break;
        // }
        // }
        // };
        //
        // AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); //
        // dialog
        // builder.setTitle(url)
        // .setMessage(getResources().getString(R.string.dialog_link))
        // .setPositiveButton(getResources().getString(R.string.action_new_tab),
        // dialogClickListener)
        // .setNegativeButton(getResources().getString(R.string.action_open),
        // dialogClickListener)
        // .setNeutralButton(getResources().getString(R.string.action_copy),
        // dialogClickListener).show();
        // }
        // } else if (result != null) {
        // if (result.getExtra() != null) {
        // final String newUrl = result.getExtra();
        // if (result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        // || result.getType() == HitTestResult.IMAGE_TYPE) {
        // DialogInterface.OnClickListener dialogClickListener = new
        // DialogInterface.OnClickListener() {
        // @Override
        // public void onClick(DialogInterface dialog, int which) {
        // switch (which) {
        // case DialogInterface.BUTTON_POSITIVE:
        // newTab(newUrl, false);
        // break;
        //
        // case DialogInterface.BUTTON_NEGATIVE:
        // mCurrentView.loadUrl(newUrl);
        // break;
        //
        // case DialogInterface.BUTTON_NEUTRAL:
        // if (API > 8) {
        // Utils.downloadFile(mActivity, newUrl,
        // mCurrentView.getUserAgent(), "attachment", false);
        // }
        // break;
        // }
        // }
        // };
        //
        // AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); //
        // dialog
        // builder.setTitle(newUrl.replace(Constants.HTTP, ""))
        // .setMessage(getResources().getString(R.string.dialog_image))
        // .setPositiveButton(getResources().getString(R.string.action_new_tab),
        // dialogClickListener)
        // .setNegativeButton(getResources().getString(R.string.action_open),
        // dialogClickListener)
        // .setNeutralButton(getResources().getString(R.string.action_download),
        // dialogClickListener).show();
        //
        // } else {
        // DialogInterface.OnClickListener dialogClickListener = new
        // DialogInterface.OnClickListener() {
        // @Override
        // public void onClick(DialogInterface dialog, int which) {
        // switch (which) {
        // case DialogInterface.BUTTON_POSITIVE:
        // newTab(newUrl, false);
        // break;
        //
        // case DialogInterface.BUTTON_NEGATIVE:
        // mCurrentView.loadUrl(newUrl);
        // break;
        //
        // case DialogInterface.BUTTON_NEUTRAL:
        // ClipboardManager clipboard = (ClipboardManager)
        // getSystemService(CLIPBOARD_SERVICE);
        // ClipData clip = ClipData.newPlainText("label", newUrl);
        // clipboard.setPrimaryClip(clip);
        //
        // break;
        // }
        // }
        // };
        //
        // AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); //
        // dialog
        // builder.setTitle(newUrl)
        // .setMessage(getResources().getString(R.string.dialog_link))
        // .setPositiveButton(getResources().getString(R.string.action_new_tab),
        // dialogClickListener)
        // .setNegativeButton(getResources().getString(R.string.action_open),
        // dialogClickListener)
        // .setNeutralButton(getResources().getString(R.string.action_copy),
        // dialogClickListener).show();
        // }
        //
        // }
        //
        // }

    }

    /**
     * This method lets the search bar know that the page is currently loading
     * and that it should display the stop icon to indicate to the user that
     * pressing it stops the page from loading
     */
    public void setIsLoading() {
        if (!mSearch.hasFocus()) {
            mIcon = mDeleteIcon;
            mSearch.setCompoundDrawables(null, null, mDeleteIcon, null);
        }
    }

    /**
     * This tells the search bar that the page is finished loading and it should
     * display the refresh icon
     */
    public void setIsFinishedLoading() {
        if (!mSearch.hasFocus()) {
            mIcon = mRefreshIcon;
            mSearch.setCompoundDrawables(null, null, mRefreshIcon, null);
        }
    }

    /**
     * handle presses on the refresh icon in the search bar, if the page is
     * loading, stop the page, if it is done loading refresh the page.
     * 
     * See setIsFinishedLoading and setIsLoading for displaying the correct icon
     */
    public void refreshOrStop() {
        if (mCurrentView != null) {
            if (mCurrentView.getProgress() < 100) {
                mCurrentView.stopLoading();
            } else {
                mCurrentView.reload();
            }
        }
    }

    @Override
    public boolean isActionBarShowing() {
        if (mActionBar != null) {
            return mActionBar.isShowing();
        } else {
            return false;
        }
    }

    // Override this, use finish() for Incognito, moveTaskToBack for Main
    public void closeActivity() {
        finish();
    }

    public class SortIgnoreCase implements Comparator<HistoryItem> {

        public int compare(HistoryItem o1, HistoryItem o2) {
            return o1.getTitle().toLowerCase(Locale.getDefault())
                    .compareTo(o2.getTitle().toLowerCase(Locale.getDefault()));
        }

    }

    // add for flint

    private boolean mQuit = false;
    private ServerSocket mServerSocket = null;
    private String mCurrentVideoUrl = null;

    protected Handler mHandler = new Handler();

    private Runnable mRefreshRunnable;
    private Runnable mRefreshFlingRunnable;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;

    private MediaRouteButton mMediaRouteButton;
    private static final String APPLICATION_ID = "~flintplayer";
    private static final String APPLICATION_URL = "http://openflint.github.io/flint-player/player.html";

    private FlintDevice mSelectedDevice;
    private FlintManager mApiClient;
    private CastListener mCastListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private RemoteMediaPlayer mMediaPlayer;
    private boolean mShouldPlayMedia;
    private MediaInfo mSelectedMedia;
    private ApplicationMetadata mAppMetadata;
    private boolean mSeeking;
    private boolean mWaitingForReconnect;

    private boolean mRelaunchApp;
    private ImageButton mPlayPauseButton;
    private ImageButton mStopFlingButton;
    private SeekBar mMediaSeekBar;
    private TextView mFlingCurrentTimeTextView;
    private TextView mFlingTotalTimeTextView;
    private TextView mFlingDeviceNameTextView;
    private TextView mFlingMediaInfoTextView;

    private TextView mVideoResolutionTextView;

    private View mFlingMediaControls;
    private View mFlingInfo;

    protected static final double VOLUME_INCREMENT = 0.05;
    protected static final double MAX_VOLUME_LEVEL = 20;

    protected static final int AFTER_SEEK_DO_NOTHING = 0;
    protected static final int AFTER_SEEK_PLAY = 1;
    protected static final int AFTER_SEEK_PAUSE = 2;

    protected static final int PLAYER_STATE_NONE = 0;
    protected static final int PLAYER_STATE_PLAYING = 1;
    protected static final int PLAYER_STATE_PAUSED = 2;
    protected static final int PLAYER_STATE_BUFFERING = 3;
    protected static final int PLAYER_STATE_FINISHED = 4;

    private static final int REFRESH_INTERVAL_MS = (int) TimeUnit.SECONDS
            .toMillis(1);

    private int mPlayerState;

    private boolean mIsUserSeeking;

    private final Map<String, String> displays = new HashMap<String, String>();

    private MediaFlingBar mMediaFlingBar;

    private CheckBox mHardwareDecoderCheckbox;

    private boolean mIsHardwareDecoder = true;

    private MediaRouteSelector buildMediaRouteSelector() {
        return new MediaRouteSelector.Builder().addControlCategory(
                FlintMediaControlIntent.categoryForFlint(APPLICATION_ID))
                .build();
    }

    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteSelected: route=" + route);
            mShouldPlayMedia = true;
            try {
                BrowserActivity.this.onRouteSelected(route);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteUnselected: route=" + route);
            mShouldPlayMedia = false;
            try {
                BrowserActivity.this.onRouteUnselected(route);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(TAG, "onRouteAdded: route=" + route);
            String display = route.toString();
            if (display != null) {
                displays.put(route.getId(), display);
            }
        }

        public void onRouteRemoved(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "onRouteRemoved: route=" + route);
            displays.remove(route.getId());
        }

        @Override
        public void onRouteChanged(MediaRouter router,
                MediaRouter.RouteInfo route) {
            Log.d(TAG, "onRouteChanged: route=" + route);
            String display = displays.get(route.getId());
            if (display != null) {
                displays.put(route.getId(), display);
            }
        }
    }

    private void initFlingServerSocket() {
        Log.e(TAG, "initFlingServerSocket!");
        mConnectionCallbacks = new ConnectionCallbacks();

        mCastListener = new CastListener();
        mMediaFlingBar = (MediaFlingBar) findViewById(R.id.media_fling);
        mMediaFlingBar.show();
        mMediaFlingBar.hide();

        Flint.FlintApi.setApplicationId(APPLICATION_ID);

        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = buildMediaRouteSelector();
        mMediaRouterCallback = new MyMediaRouterCallback();

        mMediaRouteButton = (MediaRouteButton) mMediaFlingBar
                .findViewById(R.id.media_route_button);
        mMediaRouteButton.setRouteSelector(mMediaRouteSelector);

        mPlayPauseButton = (ImageButton) mMediaFlingBar
                .findViewById(R.id.mediacontroller_play_pause);
        mPlayPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerState == PLAYER_STATE_FINISHED) {
                    mHandler.postDelayed(mRefreshRunnable, 50);
                } else if (mPlayerState == PLAYER_STATE_PAUSED
                        || mPlayerState == PLAYER_STATE_BUFFERING) {
                    onPlayClicked();
                } else if (mPlayerState == PLAYER_STATE_PLAYING) {
                    onPauseClicked();
                } else {
                    Log.e(TAG, "ignore for player state:" + mPlayerState);
                }
            }
        });

        /*
         * mStopFlingButton =
         * (ImageButton)mMediaFlingBar.findViewById(R.id.media_stop);
         * mStopFlingButton.setOnClickListener(new OnClickListener() {
         * 
         * @Override public void onClick(View v) { onStopClicked(); } });
         */

        mMediaSeekBar = (SeekBar) mMediaFlingBar
                .findViewById(R.id.mediacontroller_seekbar);
        mMediaSeekBar
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mIsUserSeeking = false;

                        if (mMediaPlayer == null) {
                            return;
                        }

                        mMediaSeekBar.setSecondaryProgress(0);
                        onSeekBarMoved(TimeUnit.SECONDS.toMillis(seekBar
                                .getProgress()));

                        refreshSeekPosition(TimeUnit.SECONDS.toMillis(seekBar
                                .getProgress()), mMediaPlayer
                                .getStreamDuration());
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mIsUserSeeking = true;

                        if (mMediaPlayer == null) {
                            return;
                        }

                        refreshSeekPosition(TimeUnit.SECONDS.toMillis(seekBar
                                .getProgress()), mMediaPlayer
                                .getStreamDuration());

                        mMediaSeekBar.setSecondaryProgress(seekBar
                                .getProgress());
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                            int progress, boolean fromUser) {

                        if (mMediaPlayer == null) {
                            return;
                        }

                        refreshSeekPosition(TimeUnit.SECONDS.toMillis(seekBar
                                .getProgress()), mMediaPlayer
                                .getStreamDuration());
                    }
                });

        mFlingCurrentTimeTextView = (TextView) mMediaFlingBar
                .findViewById(R.id.mediacontroller_time_current);
        mFlingTotalTimeTextView = (TextView) mMediaFlingBar
                .findViewById(R.id.mediacontroller_time_total);

        mFlingMediaControls = mMediaFlingBar
                .findViewById(R.id.mediacontroller_control);
        mFlingInfo = mMediaFlingBar.findViewById(R.id.fling_info);

        mFlingDeviceNameTextView = (TextView) mMediaFlingBar
                .findViewById(R.id.fling_device_name);
        mFlingMediaInfoTextView = (TextView) mMediaFlingBar
                .findViewById(R.id.media_info);

        mVideoResolutionTextView = (TextView) mMediaFlingBar
                .findViewById(R.id.resolution);

        mVideoResolutionTextView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (listDialog != null) {
                    listDialog.dismiss();
                }
                Log.e(TAG, "onClick!");
                initListDialog();

            }

        });

        mVideoResolutionTextView.setVisibility(View.INVISIBLE);

        setPlayerState(PLAYER_STATE_NONE);

        mRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "show media cast control?![" + displays.size() + "]");
                if (displays.size() > 0) {

                    final String url = mCurrentView.getUrl();

                    new Thread(new Runnable() {

                        @Override
                        public void run() {

                            if (mCurrentView == null) {
                                return;
                            }

                            // TODO Auto-generated method stub
                            List<NameValuePair> param = new ArrayList<NameValuePair>();

                            param.add(new BasicNameValuePair("apptoken",
                                    "3e52201f5037ad9bd8e389348916bd3a"));
                            param.add(new BasicNameValuePair("method",
                                    "core.video.realurl"));
                            param.add(new BasicNameValuePair("packageName",
                                    "com.infthink.test"));
                            param.add(new BasicNameValuePair("url", url));

                            Log.e(TAG, "get real video url:" + url);

                            SendHttpsPOST("https://play.aituzi.com", param,
                                    null);

                        }

                    }).start();

                    if (mVideoResolutionTextView != null
                            && mVideoResolutionTextView.getVisibility() == View.VISIBLE) {
                        Log.e(TAG,
                                "Ignore this video url for real video url is present!");
                        return;
                    }

                    Toast.makeText(mContext, mCurrentVideoUrl,
                            Toast.LENGTH_SHORT).show();

                    MediaMetadata metadata = new MediaMetadata(
                            MediaMetadata.MEDIA_TYPE_MOVIE);
                    mSelectedMedia = new MediaInfo.Builder(mCurrentVideoUrl)
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setContentType("video/mp4").setMetadata(metadata)
                            .build();

                    mMediaFlingBar.show();

                    Log.e(TAG, "should show!");
                    if (mApiClient != null && mApiClient.isConnected()) {
                        if (mMediaPlayer != null) {
                            playMedia(mSelectedMedia);
                        }

                    }
                }
            }
        };

        mRefreshFlingRunnable = new Runnable() {
            @Override
            public void run() {
                if (mQuit) {
                    Log.e(TAG, "mRefreshFlingRunnable:quit!");
                    return;
                }
                onRefreshEvent();
                startRefreshTimer();
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mServerSocket = new ServerSocket(3334, 10,
                            InetAddress.getByName("127.0.0.1"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while (!mQuit) {
                    try {
                        if (mServerSocket == null) {
                            try {
                                mServerSocket = new ServerSocket(3334, 10,
                                        InetAddress.getByName("127.0.0.1"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        Socket s = mServerSocket.accept();

                        BufferedReader br = new BufferedReader(
                                new InputStreamReader(s.getInputStream()));
                        String line = br.readLine();
                        Log.e(TAG, "!来自服务器的数据：[" + line + "]");

                        mCurrentVideoUrl = line;

                        br.close();
                        s.close();
                        mHandler.postDelayed(mRefreshRunnable, 1000);

                        // Toast.makeText(BrowserActivity.this,
                        // "获得视频地址:[" + line + "]", Toast.LENGTH_SHORT)
                        // .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {

                    }
                }

                Log.e(TAG, "Quit Fling Socket Server Thread!");
                try {
                    mServerSocket.close();
                    mServerSocket = null;
                } catch (Exception e) {
                }
            }
        }).start();

        mFlintMsgChannel = new FlintMsgChannel() {
            @Override
            public void onMessageReceived(FlintDevice flingDevice,
                    String namespace, final String message) {
                super.onMessageReceived(flingDevice, namespace, message);

                // show received custom messages.
                Log.d(TAG, "onMessageReceived: " + message);

                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            JSONObject obj = new JSONObject(message);
                            mIsHardwareDecoder = obj
                                    .getBoolean("isHardwareDecoder");

                            mHardwareDecoderCheckbox
                                    .setChecked(mIsHardwareDecoder);

                            mHardwareDecoderCheckbox
                                    .setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        };

        mHardwareDecoderCheckbox = (CheckBox) mMediaFlingBar
                .findViewById(R.id.device_hardware_decoder);
        mHardwareDecoderCheckbox.setVisibility(View.GONE);
        mHardwareDecoderCheckbox
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                            boolean isChecked) {
                        // TODO Auto-generated method stub
                        if (mApiClient != null) {
                            Log.e(TAG, "setHardwareDecoder:" + isChecked);
                            setHardwareDecoder(isChecked);
                        }
                    }

                });
    }

    /**
     * Set custom message to device. let device use hardware decoder or not
     * 
     * @param flag
     */
    private void setHardwareDecoder(boolean flag) {
        if (mApiClient == null || !mApiClient.isConnected()) {
            return;
        }

        mFlintMsgChannel.setHardwareDecoder(mApiClient, flag);
    }

    protected void onRouteSelected(RouteInfo route) {
        Log.d(TAG, "onRouteSelected: " + route + " url:" + mCurrentVideoUrl);

        if (mCurrentVideoUrl == null) {
            Log.d(TAG, "url is " + mCurrentVideoUrl + " ignore it!");
            Toast.makeText(this, "url is null!ignore it!", Toast.LENGTH_SHORT)
                    .show();
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
            return;
        }

        MediaMetadata metadata = new MediaMetadata(
                MediaMetadata.MEDIA_TYPE_MOVIE);
        mSelectedMedia = new MediaInfo.Builder(mCurrentVideoUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("video/mp4").setMetadata(metadata).build();

        FlintDevice device = FlintDevice.getFromBundle(route.getExtras());
        setSelectedDevice(device);
        updateButtonStates();
    }

    protected void onRouteUnselected(RouteInfo route) {
        Log.d(TAG, "onRouteUnselected: " + route);
        setSelectedDevice(null);
        mAppMetadata = null;
        clearMediaState();
        updateButtonStates();
    }

    private void setSelectedDevice(FlintDevice device) {
        mSelectedDevice = device;

        if (mSelectedDevice != null) {
            // mFlingDeviceNameTextView.setText(mSelectedDevice.getFriendlyName());
        }

        if (mSelectedDevice == null) {
            Log.d(TAG, "destroy controller");
            onStopAppClicked();
            detachMediaPlayer();
            if ((mApiClient != null) && mApiClient.isConnected()) {
                mApiClient.disconnect();
            }
            mApiClient = null;
        } else {
            Log.d(TAG, "acquiring controller for " + mSelectedDevice);
            try {
                Flint.FlintOptions.Builder apiOptionsBuilder = Flint.FlintOptions
                        .builder(mSelectedDevice, mCastListener);
                // apiOptionsBuilder.setVerboseLoggingEnabled(true);

                mApiClient = new FlintManager.Builder(this)
                        .addApi(Flint.API, apiOptionsBuilder.build())
                        .addConnectionCallbacks(mConnectionCallbacks).build();
                mApiClient.connect();
            } catch (IllegalStateException e) {
                Log.w(TAG, "error while creating a device controller", e);
                // showErrorDialog(getString(R.string.error_no_controller));
            }
        }
    }

    private class ConnectionCallbacks implements
            FlintManager.ConnectionCallbacks {
        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "ConnectionCallbacks.onConnectionSuspended");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO: need to disable all controls, and possibly display
                    // a
                    // "reconnecting..." dialog or overlay
                    detachMediaPlayer();
                    updateButtonStates();
                    mWaitingForReconnect = true;
                }
            });
        }

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.d(TAG, "onConnectionFailed");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateButtonStates();
                    clearMediaState();
                    cancelRefreshTimer();
                    // showErrorDialog(getString(R.string.error_no_device_connection));
                }
            });
        }

        @Override
        public void onConnected(final Bundle connectionHint) {
            Log.d(TAG, "ConnectionCallbacks.onConnected");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mApiClient == null || !mApiClient.isConnected()) {
                        // We got disconnected while this runnable was pending
                        // execution.
                        Log.d(TAG,
                                "ConnectionCallbacks.onConnected. ignore it!");
                        return;
                    }

                    onLaunchAppClicked();
                }
            });
        }
    }

    protected final void setApplicationStatus(String statusText) {
        if (mSelectedDevice == null) {
            return;
        }

        // mFlingDeviceNameTextView.setText(mSelectedDevice.getFriendlyName());

        if (mSelectedMedia == null) {
            return;
        }

        mFlingMediaInfoTextView.setText(mSelectedMedia.getContentId());
    }

    private class CastListener extends Flint.Listener {
        @Override
        public void onVolumeChanged() {
            refreshDeviceVolume(Flint.FlintApi.getVolume(mApiClient),
                    Flint.FlintApi.isMute(mApiClient));
        }

        @Override
        public void onApplicationStatusChanged() {
            String status = Flint.FlintApi.getApplicationStatus(mApiClient);
            Log.d(TAG, "onApplicationStatusChanged; status=" + status);
            setApplicationStatus(status);
        }

        @Override
        public void onApplicationDisconnected(int statusCode) {
            Log.d(TAG, "onApplicationDisconnected: statusCode=" + statusCode);
            mAppMetadata = null;
            detachMediaPlayer();
            clearMediaState();
            updateButtonStates();
            // if (statusCode != FlintStatusCodes.SUCCESS) {
            // This is an unexpected disconnect.
            // setApplicationStatus(getString(R.string.status_app_disconnected));
            // }
        }
    }

    private final class ApplicationConnectionResultCallback implements
            ResultCallback<Flint.ApplicationConnectionResult> {
        private final String mClassTag;

        public ApplicationConnectionResultCallback(String suffix) {
            mClassTag = TAG;
        }

        @Override
        public void onResult(ApplicationConnectionResult result) {
            Status status = result.getStatus();
            Log.d(mClassTag,
                    "ApplicationConnectionResultCallback.onResult: statusCode"
                            + status.getStatusCode());
            if (status.isSuccess()) {
                ApplicationMetadata applicationMetadata = result
                        .getApplicationMetadata();
                // String sessionId = result.getSessionId();
                String applicationStatus = result.getApplicationStatus();
                boolean wasLaunched = result.getWasLaunched();
                // Log.d(mClassTag, "application name: " +
                // applicationMetadata.getName()
                // + ", status: " + applicationStatus + ", sessionId: " +
                // sessionId
                // + ", wasLaunched: " + wasLaunched);
                setApplicationStatus(applicationStatus);
                attachMediaPlayer();
                mAppMetadata = applicationMetadata;
                startRefreshTimer();
                updateButtonStates();
                Log.d(mClassTag, "mShouldPlayMedia is " + mShouldPlayMedia);
                if (mShouldPlayMedia) {
                    mShouldPlayMedia = false;
                    Log.d(mClassTag, "now loading media");
                    playMedia(mSelectedMedia);
                } else {
                    // Synchronize with the receiver's state.
                    Log.d(mClassTag, "requesting current media status");
                    mMediaPlayer
                            .requestStatus(mApiClient)
                            .setResultCallback(
                                    new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                                        @Override
                                        public void onResult(
                                                MediaChannelResult result) {
                                            Status status = result.getStatus();
                                            if (!status.isSuccess()) {
                                                Log.w(mClassTag,
                                                        "Unable to request status: "
                                                                + status.getStatusCode());
                                            }
                                        }
                                    });
                }
            } else {
                Log.d(mClassTag, "status is not success!");
                // showErrorDialog(getString(R.string.error_app_launch_failed));
            }
        }
    }

    private void clearMediaState() {
        mSeeking = false;

        setCurrentMediaMetadata(null, null, null);
        refreshPlaybackPosition(0, 0);
    }

    private void attachMediaPlayer() {
        if (mMediaPlayer != null) {
            return;
        }

        mMediaPlayer = new RemoteMediaPlayer();
        mMediaPlayer
                .setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {

                    @Override
                    public void onStatusUpdated() {

                        MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
                        if (mediaStatus != null) {
                            Log.d(TAG, "MediaControlChannel.onStatusUpdated["
                                    + mediaStatus.getPlayerState() + "]");
                        } else {
                            Log.d(TAG, "MediaControlChannel.onStatusUpdated");
                        }

                        // If item has ended, clear metadata.
                        if ((mediaStatus != null)
                                && (mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_IDLE)) {
                            clearMediaState();
                        }

                        if ((mediaStatus != null)
                                && (mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING)) {
                            if (mSelectedDevice != null) {
                                mFlingDeviceNameTextView
                                        .setText(mSelectedDevice
                                                .getFriendlyName());
                            }
                        }

                        if ((mediaStatus != null)
                                && (mediaStatus.getPlayerState() != MediaStatus.PLAYER_STATE_BUFFERING)) {

                            updatePlaybackPosition();
                        } else if ((mediaStatus != null)
                                && (mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_BUFFERING)) {
                            if (mSelectedDevice != null) {
                                mFlingDeviceNameTextView
                                        .setText(mSelectedDevice
                                                .getFriendlyName()
                                                + "(Loading...)");
                            }
                        }

                        updateStreamVolume();
                        updateButtonStates();
                    }
                });

        mMediaPlayer
                .setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                    @Override
                    public void onMetadataUpdated() {
                        Log.d(TAG, "MediaControlChannel.onMetadataUpdated");
                        String title = null;
                        String artist = null;
                        Uri imageUrl = null;

                        MediaInfo mediaInfo = mMediaPlayer.getMediaInfo();
                        if (mediaInfo != null) {
                            MediaMetadata metadata = mediaInfo.getMetadata();
                            if (metadata != null) {
                                title = metadata
                                        .getString(MediaMetadata.KEY_TITLE);

                                artist = metadata
                                        .getString(MediaMetadata.KEY_ARTIST);
                                if (artist == null) {
                                    artist = metadata
                                            .getString(MediaMetadata.KEY_STUDIO);
                                }

                                List<WebImage> images = metadata.getImages();
                                if ((images != null) && !images.isEmpty()) {
                                    WebImage image = images.get(0);
                                    imageUrl = image.getUrl();
                                }
                            }
                            setCurrentMediaMetadata(title, artist, imageUrl);
                        }
                    }
                });

        try {
            Flint.FlintApi.setMessageReceivedCallbacks(mApiClient,
                    mMediaPlayer.getNamespace(), mMediaPlayer);

            // use this channel to send message channel.
            Flint.FlintApi.setMessageReceivedCallbacks(mApiClient,
                    mFlintMsgChannel.getNamespace(), mFlintMsgChannel);

        } catch (IOException e) {
            Log.w(TAG, "Exception while launching application", e);
        }
    }

    private void reattachMediaPlayer() {
        if ((mMediaPlayer != null) && (mApiClient != null)) {
            try {
                Flint.FlintApi.setMessageReceivedCallbacks(mApiClient,
                        mMediaPlayer.getNamespace(), mMediaPlayer);

                // use this channel to send message channel.
                Flint.FlintApi.setMessageReceivedCallbacks(mApiClient,
                        mFlintMsgChannel.getNamespace(), mFlintMsgChannel);
            } catch (IOException e) {
                Log.w(TAG, "Exception while launching application", e);
            }
        }
    }

    private void detachMediaPlayer() {
        if ((mMediaPlayer != null) && (mApiClient != null)) {
            try {
                Flint.FlintApi.removeMessageReceivedCallbacks(mApiClient,
                        mMediaPlayer.getNamespace());

                Flint.FlintApi.removeMessageReceivedCallbacks(mApiClient,
                        mFlintMsgChannel.getNamespace());
            } catch (IOException e) {
                Log.w(TAG, "Exception while launching application", e);
            }
        }
        mMediaPlayer = null;
    }

    private void updateFlingDispInfo(boolean show) {
        if (show) {
            mFlingInfo.setVisibility(View.VISIBLE);
        } else {
            mFlingInfo.setVisibility(View.GONE);
            mFlingDeviceNameTextView.setText("");
            mFlingMediaInfoTextView.setText("");
        }
    }

    private void updateButtonStates() {
        boolean hasDeviceConnection = (mApiClient != null)
                && mApiClient.isConnected();
        boolean hasAppConnection = (mAppMetadata != null);
        boolean hasMediaConnection = (mMediaPlayer != null);
        boolean hasMedia = false;

        if (hasMediaConnection) {
            MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
            if (mediaStatus != null) {
                int mediaPlayerState = mediaStatus.getPlayerState();
                int playerState = PLAYER_STATE_NONE;
                if (mediaPlayerState == MediaStatus.PLAYER_STATE_PAUSED) {
                    playerState = PLAYER_STATE_PAUSED;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_PLAYING) {
                    playerState = PLAYER_STATE_PLAYING;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_BUFFERING) {
                    playerState = PLAYER_STATE_BUFFERING;
                } else if (mediaPlayerState == MediaStatus.PLAYER_STATE_IDLE) {
                    playerState = PLAYER_STATE_FINISHED;

                    mSeeking = false;

                    refreshPlaybackPosition(0, mMediaPlayer.getStreamDuration());
                }
                setPlayerState(playerState);

                hasMedia = mediaStatus.getPlayerState() != MediaStatus.PLAYER_STATE_IDLE;
                // mStopButton.setEnabled(hasMedia);

                updateFlingDispInfo(true);

                setSeekBarEnabled(playerState != PLAYER_STATE_FINISHED
                        && playerState != PLAYER_STATE_NONE);
            }
        } else {
            setPlayerState(PLAYER_STATE_NONE);

            updateFlingDispInfo(false);
            // mStopButton.setEnabled(false);

            setSeekBarEnabled(false);
        }

        /*
         * mLaunchAppButton.setEnabled(hasDeviceConnection &&
         * !hasAppConnection); mJoinAppButton.setEnabled(hasDeviceConnection &&
         * !hasAppConnection); mLeaveAppButton.setEnabled(hasDeviceConnection &&
         * hasAppConnection); mStopAppButton.setEnabled(hasDeviceConnection &&
         * hasAppConnection); mAutoplayCheckbox.setEnabled(hasDeviceConnection
         * && hasAppConnection);
         */

        // mPlayPauseButton.setEnabled(hasMediaConnection);

        /*
         * setDeviceVolumeControlsEnabled(hasDeviceConnection);
         * setStreamVolumeControlsEnabled(hasMediaConnection);
         */
    }

    protected final void setCurrentMediaMetadata(String title, String subtitle,
            Uri imageUrl) {
    }

    protected final void refreshPlaybackPosition(long position, long duration) {
        // Log.e(TAG, "refreshPlaybackPosition:position[" + position +
        // "]duration[" + duration + "]mIsUserSeeking[" + mIsUserSeeking + "]");
        if (!mIsUserSeeking) {
            if (position == 0) {
                mFlingTotalTimeTextView.setText("N/A");
                mMediaSeekBar.setProgress(0);
            } else if (position > 0) {
                mMediaSeekBar.setProgress((int) TimeUnit.MILLISECONDS
                        .toSeconds(position));
            }
            mFlingCurrentTimeTextView.setText(formatTime(position));
        }

        if (duration == 0) {
            mMediaSeekBar.setProgress(0);
            mFlingTotalTimeTextView.setText("N/A");
            mMediaSeekBar.setMax(0);
        } else if (duration > 0) {
            mFlingTotalTimeTextView.setText(formatTime(duration));
            if (!mIsUserSeeking) {
                mMediaSeekBar.setMax((int) TimeUnit.MILLISECONDS
                        .toSeconds(duration));
            }
        }
    }

    protected final String getReceiverApplicationId() {
        return APPLICATION_ID;
    }

    protected final boolean getRelaunchApp() {
        return mRelaunchApp;
    }

    /*
     * Connects to the device (if necessary), and then casts the currently
     * selected video.
     */
    protected void onPlayMedia(final MediaInfo media) {
        mSelectedMedia = media;

        if (mAppMetadata == null) {
            return;
        }

        playMedia(mSelectedMedia);
    }

    protected void onLaunchAppClicked() {

        if (mApiClient == null) {
            return;
        }
        Log.e(TAG, "onLaunchAppClicked!");
        Flint.FlintApi.launchApplication(mApiClient, APPLICATION_URL)
                .setResultCallback(
                        new ApplicationConnectionResultCallback("LaunchApp"));
    }

    /*
     * Begins playback of the currently selected video.
     */
    private void playMedia(MediaInfo media) {
        Log.d(TAG, "playMedia: " + media);
        if (media == null) {
            return;
        }
        if (mMediaPlayer == null) {
            Log.e(TAG, "Trying to play a video with no active media session");
            return;
        }

        if (mSelectedDevice != null) {
            mFlingDeviceNameTextView.setText(mSelectedDevice.getFriendlyName()
                    + "(Loading...)");
        }

        mMediaPlayer
                .load(mApiClient, media, isAutoplayChecked())
                .setResultCallback(
                        new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                            @Override
                            public void onResult(MediaChannelResult result) {
                                if (!result.getStatus().isSuccess()) {
                                    Log.e(TAG, "Failed to load media.");
                                }
                            }
                        });
    }

    private void updatePlaybackPosition() {
        if (mMediaPlayer == null) {
            return;
        }

        refreshPlaybackPosition(mMediaPlayer.getApproximateStreamPosition(),
                mMediaPlayer.getStreamDuration());
    }

    private void updateStreamVolume() {
        if (mMediaPlayer == null) {
            return;
        }
        MediaStatus mediaStatus = mMediaPlayer.getMediaStatus();
        if (mediaStatus != null) {
            double streamVolume = mediaStatus.getStreamVolume();
            boolean muteState = mediaStatus.isMute();
            refreshStreamVolume(streamVolume, muteState);
        }
    }

    protected final void cancelRefreshTimer() {
        RuntimeException ex = new RuntimeException();
        ex.printStackTrace();

        mHandler.removeCallbacks(mRefreshFlingRunnable);
    }

    protected final void refreshDeviceVolume(double percent, boolean muted) {
        /*
         * if (!mIsUserAdjustingVolume) { mDeviceVolumeBar.setProgress((int)
         * (percent * MAX_VOLUME_LEVEL)); }
         * mDeviceMuteCheckBox.setChecked(muted);
         */
    }

    protected final void startRefreshTimer() {
        mHandler.postDelayed(mRefreshFlingRunnable, REFRESH_INTERVAL_MS);
    }

    protected final void refreshStreamVolume(double percent, boolean muted) {
        /*
         * if (!mIsUserAdjustingVolume) { mStreamVolumeBar.setProgress((int)
         * (percent * MAX_VOLUME_LEVEL)); }
         * mStreamMuteCheckBox.setChecked(muted);
         */
    }

    protected final boolean isAutoplayChecked() {
        return true;
    }

    protected void onStopAppClicked() {
        if (mApiClient == null) {
            return;
        }

        try {
            Flint.FlintApi.stopApplication(mApiClient).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status result) {
                            if (result.isSuccess()) {
                                mAppMetadata = null;
                                detachMediaPlayer();
                                updateButtonStates();
                            } else {
                                // showErrorDialog(getString(R.string.error_app_stop_failed));
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onPlayClicked() {
        if (mMediaPlayer == null) {
            return;
        }
        try {
            mMediaPlayer.play(mApiClient);
        } catch (Exception e) {
            Log.w(TAG, "Unable to play", e);
            // showErrorDialog(e.getMessage());
        }
    }

    protected void onPauseClicked() {
        if (mMediaPlayer == null) {
            return;
        }
        try {
            mMediaPlayer.pause(mApiClient);
        } catch (Exception e) {
            Log.w(TAG, "Unable to pause", e);
            // showErrorDialog(e.getMessage());
        }
    }

    protected void onStopClicked() {
        if (mMediaPlayer == null) {
            return;
        }
        try {
            mMediaPlayer.stop(mApiClient);
        } catch (Exception e) {
            Log.w(TAG, "Unable to stop");
            // showErrorDialog(e.getMessage());
        }

        mShouldPlayMedia = false;
        onRouteUnselected(null);

        mMediaFlingBar.hide();
    }

    protected void onSeekBarMoved(long position) {
        if (mMediaPlayer == null) {
            return;
        }

        refreshPlaybackPosition(position, -1);

        int behavior = getSeekBehavior();

        int resumeState;
        switch (behavior) {
        case AFTER_SEEK_PLAY:
            resumeState = RemoteMediaPlayer.RESUME_STATE_PLAY;
            break;
        case AFTER_SEEK_PAUSE:
            resumeState = RemoteMediaPlayer.RESUME_STATE_PAUSE;
            break;
        case AFTER_SEEK_DO_NOTHING:
        default:
            resumeState = RemoteMediaPlayer.RESUME_STATE_UNCHANGED;
        }
        mSeeking = true;
        try {
            Log.e(TAG, "seek: position[" + position + "]state[" + resumeState
                    + "]");

            mMediaPlayer.seek(mApiClient, position, resumeState)
                    .setResultCallback(
                            new ResultCallback<MediaChannelResult>() {
                                @Override
                                public void onResult(MediaChannelResult result) {
                                    Status status = result.getStatus();
                                    if (status.isSuccess()) {
                                        mSeeking = false;
                                    } else {
                                        mSeeking = false;
                                        mPlayPauseButton.setEnabled(true);
                                        Log.w(TAG,
                                                "Unable to seek: "
                                                        + status.getStatusCode());
                                    }
                                }

                            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onDeviceVolumeBarMoved(int volume) {
        if (mApiClient == null) {
            return;
        }
        try {
            Flint.FlintApi.setVolume(mApiClient, volume / MAX_VOLUME_LEVEL);
        } catch (IOException e) {
            Log.w(TAG, "Unable to change volume");
        } catch (IllegalStateException e) {
            // showErrorDialog(e.getMessage());
        }
    }

    protected final int getSeekBehavior() {
        if (mPlayerState == PLAYER_STATE_PLAYING) {
            return AFTER_SEEK_PLAY;
        }

        if (mPlayerState == PLAYER_STATE_PAUSED) {
            return AFTER_SEEK_PAUSE;
        }

        return AFTER_SEEK_DO_NOTHING;
    }

    // ValueCallback<String> callback = null;

    protected void onRefreshEvent() {
        if (!mSeeking) {
            updatePlaybackPosition();
        }
        updateStreamVolume();
        updateButtonStates();

        /*
         * if (mCurrentView != null) { if (callback == null) { callback = new
         * ValueCallback<String>() {
         * 
         * @Override public void onReceiveValue(String result) { Log.e(TAG,
         * "result:" + result); } }; } Log.e(TAG, "evaluateJavascript!"); String
         * GET_VIDEO_URL_SCRIPT =
         * "function getVideoUrl() {var videos = document.getElementsByTagName('video'); if (videos != null && videos[0] != null) {return videos[0].src;} else {  return 'haha';}}; getVideoUrl()"
         * ; mCurrentView.getWebView().evaluateJavascript(GET_VIDEO_URL_SCRIPT,
         * callback); }
         */
    }

    protected final void setPlayerState(int playerState) {
        mPlayerState = playerState;
        if (mPlayerState == PLAYER_STATE_PAUSED) {
            mPlayPauseButton.setImageResource(R.drawable.mediacontroller_play);
        } else if (mPlayerState == PLAYER_STATE_PLAYING) {
            mPlayPauseButton.setImageResource(R.drawable.mediacontroller_pause);
        } else {
            mPlayPauseButton.setImageResource(R.drawable.mediacontroller_play);
        }

        mPlayPauseButton.setEnabled((mPlayerState == PLAYER_STATE_PAUSED)
                || (mPlayerState == PLAYER_STATE_PLAYING)
                || mPlayerState == PLAYER_STATE_FINISHED
                || mPlayerState == PLAYER_STATE_BUFFERING);
    }

    protected final void setSeekBarEnabled(boolean enabled) {
        mMediaSeekBar.setEnabled(enabled);
    }

    private String formatTime(long millisec) {
        int seconds = (int) (millisec / 1000);
        int hours = seconds / (60 * 60);
        seconds %= (60 * 60);
        int minutes = seconds / 60;
        seconds %= 60;

        String time;
        if (hours > 0) {
            time = String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            time = String.format("%d:%02d", minutes, seconds);
        }
        return time;
    }

    private void onVolumeChange(double volumeIncrement) {
        if (mMediaPlayer == null) {
            return;
        }

        Log.e(TAG, "volumeIncrement:" + volumeIncrement);

        try {
            double v = mMediaPlayer.getMediaStatus().getStreamVolume();
            Log.e(TAG, "volumeIncrement:" + volumeIncrement + " v[" + v + "]");
            v += volumeIncrement;
            if (v > 1.0) {
                v = 1.0;
            } else if (v < 0) {
                v = 0.0;
            }

            mMediaPlayer.setStreamVolume(mApiClient, v).setResultCallback(
                    new ResultCallback<MediaChannelResult>() {
                        @Override
                        public void onResult(MediaChannelResult result) {
                            Status status = result.getStatus();
                            if (!status.isSuccess()) {
                                Log.w(TAG,
                                        "Unable to set volume: "
                                                + status.getStatusCode());
                            }
                        }

                    });
        } catch (IllegalStateException e) {
            // showErrorDialog(e.getMessage());
        }
    }

    protected final void refreshSeekPosition(long position, long duration) {
        mFlingCurrentTimeTextView.setText(formatTime(position));
    }

    private class MyHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            // TODO Auto-generated method stub
            return true;
        }
    }

    private class MyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // TODO Auto-generated method stub
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)

        throws CertificateException {
            // TODO Auto-generated method stub
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public String SendHttpsPOST(String url, List<NameValuePair> param,
            String data) {
        String result = null;

        Log.e(TAG, "SendHttpsPOST!");

        // 使用此工具可以将键值对编码成"Key=Value&amp;Key2=Value2&amp;Key3=Value3&rdquo;形式的请求参数
        String requestParam = URLEncodedUtils.format(param, "UTF-8");

        try {
            // 设置SSLContext
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[] { new MyTrustManager() },
                    null);

            Log.e(TAG, "SendHttpsPOST! 1");
            HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext
                    .getSocketFactory());
            HttpsURLConnection
                    .setDefaultHostnameVerifier(new MyHostnameVerifier());

            Log.e(TAG, "SendHttpsPOST! 2");
            // 打开连接
            // 要发送的POST请求url?Key=Value&amp;Key2=Value2&amp;Key3=Value3的形式
            // URL requestUrl = new URL(url + "?" + requestParam);
            URL requestUrl = new URL(url);
            HttpsURLConnection httpsConn = (HttpsURLConnection) requestUrl
                    .openConnection();

            Log.e(TAG, "SendHttpsPOST! 3");

            // 设置套接工厂
            // httpsConn.setSSLSocketFactory(sslcontext.getSocketFactory());

            // 加入数据
            httpsConn.setRequestMethod("POST");
            Log.e(TAG, "SendHttpsPOST! 4");

            httpsConn.setDoOutput(true);
            httpsConn.setDoInput(true);
            httpsConn.setUseCaches(false);
            httpsConn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            // Form the POST parameters
            // StringBuilder content = new StringBuilder();
            // boolean first = true;
            // Iterator iterator = param.iterator();
            // Entry parameter = (Entry) iterator.next();
            // try {
            // while (parameter != null) {
            // if (!first) {
            // content.append("&");
            // }
            // content.append(
            // URLEncoder.encode((String) parameter.getKey(),
            // "UTF-8")).append("=");
            // content.append(URLEncoder.encode(
            // (String) parameter.getValue(), "UTF-8"));
            // first = false;
            // parameter = (Entry) iterator.next();
            // }
            // } catch (NoSuchElementException e) {
            // e.printStackTrace();
            // }

            // send the POST request to server
            OutputStream outputStream = null;
            try {
                outputStream = httpsConn.getOutputStream();
                outputStream.write(requestParam.toString().getBytes("utf-8"));
                outputStream.flush();
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }

            Log.e(TAG, "SendHttpsPOST! 5");
            // DataOutputStream out = new DataOutputStream(
            // httpsConn.getOutputStream());
            // Log.e(TAG, "SendHttpsPOST! 6");
            // if (data != null)
            // out.writeBytes(data);
            //
            // out.flush();
            // out.close();

            Log.e(TAG, "SendHttpsPOST! 6.1");

            // 获取输入流
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    httpsConn.getInputStream()));
            Log.e(TAG, "SendHttpsPOST! 7");
            int code = httpsConn.getResponseCode();
            Log.e(TAG, "SendHttpsPOST! 8");
            if (HttpsURLConnection.HTTP_OK == code) {
                String temp = in.readLine();
                /* 连接成一个字符串 */
                while (temp != null) {
                    if (result != null)
                        result += temp;
                    else
                        result = temp;
                    temp = in.readLine();
                }

                Log.e(TAG, "SendHttpsPOST:response[" + result + "]");

                // ready to processs video urls!
                processVideoUrls(result);
            }

            Log.e(TAG, "SendHttpsPOST![" + code + "]");
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Get video url list
     * 
     * @param result
     */
    void processVideoUrls(String result) {
        videoUrls.clear();

        // {"status":200,"data":[{"url":"http:\/\/v.youku.com\/v_show\/id_XODUwMDM0NDUy.html?from=y1.3-tv-grid-1007-9910.86804.1-1","id":"","num":"","vid":"","source":"youku","sourceName":"\u4f18\u9177\u89c6\u9891","playUrl":{"HD":["http:\/\/pl.youku.com\/playlist\/m3u8?ts=1427076444&keyframe=1&vid=XODUwMDM0NDUy&type=hd2&sid=442707644496121c78ca6&token=5158&oip=1008521675&ep=v4TS4z2PnxtMZfqTd5f%2FdgrHMEbE4Lhvk9YdQoGTJsv7lbbElD2WtWp9mT7DI5SF&did=3f2189e6a744e68de6761a20ceaf379aa8acfad4&ctype=21&ev=1"],"SD":["http:\/\/pl.youku.com\/playlist\/m3u8?ts=1427076444&keyframe=1&vid=XODUwMDM0NDUy&type=mp4&sid=442707644496121c78ca6&token=5158&oip=1008521675&ep=v4TS4z2PnxtMZfqTd5f%2FdgrHMEbE4Lhvk9YdQoGTJsv7lbbElD2WtWp9mT7DI5SF&did=3f2189e6a744e68de6761a20ceaf379aa8acfad4&ctype=21&ev=1"]}}]}
        try {
            JSONObject obj = new JSONObject(result);
            String status = obj.getString("status");

            if ("200".equals(status)) {
                JSONArray data = obj.getJSONArray("data");

                JSONObject r = data.getJSONObject(0);
                final String url = r.getString("url");
                JSONObject playUrl = r.getJSONObject("playUrl");

                try {
                    JSONArray Smooth = playUrl.getJSONArray("Smooth");
                    videoUrls.put(getString(R.string.resolution_Smooth),
                            Smooth.getString(0));
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                try {
                    JSONArray SD = playUrl.getJSONArray("SD");
                    videoUrls.put(getString(R.string.resolution_SD),
                            SD.getString(0));
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                try {
                    JSONArray HD = playUrl.getJSONArray("HD");
                    videoUrls.put(getString(R.string.resolution_HD),
                            HD.getString(0));
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                try {
                    JSONArray Ultraclear = playUrl.getJSONArray("Ultraclear");
                    videoUrls.put(getString(R.string.resolution_Ultraclear),
                            Ultraclear.getString(0));
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                try {
                    JSONArray Bluray = playUrl.getJSONArray("Bluray");
                    videoUrls.put(getString(R.string.resolution_Bluray),
                            Bluray.getString(0));
                } catch (Exception e) {
                    // e.printStackTrace();
                }

                Log.e(TAG,
                        "playUrl:" + playUrl.toString() + "["
                                + videoUrls.toString() + "]");

                if (videoUrls.size() > 0) {
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub

                            mSiteUrl = url;

                            if (listDialog != null) {
                                listDialog.setDialogTitile(url);
                            }

                            Toast.makeText(mContext,
                                    "Get real video url OK!!!",
                                    Toast.LENGTH_SHORT).show();

                            mVideoResolutionTextView
                                    .setVisibility(View.VISIBLE);
                        }

                    });
                } else {
                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            // TODO Auto-generated method stub

                            mVideoResolutionTextView
                                    .setVisibility(View.INVISIBLE);
                        }

                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub

                    mVideoResolutionTextView.setVisibility(View.INVISIBLE);
                }

            });
        }
    }

    private CustomDialog listDialog;
    Map<String, String> videoUrls = new HashMap<String, String>();
    ArrayList<String> videoList = new ArrayList<String>();

    private String mSiteUrl;

    /**
     * 初始化列表框
     */
    private void initListDialog() {
        listDialog = CustomDialog.createListDialog(this,
                new OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int arg2, long arg3) {
                        listDialog.dismiss();

                        mVideoResolutionTextView.setText(videoList.get(arg2));

                        mCurrentVideoUrl = videoUrls.get(videoList.get(arg2));

                        MediaMetadata metadata = new MediaMetadata(
                                MediaMetadata.MEDIA_TYPE_MOVIE);
                        mSelectedMedia = new MediaInfo.Builder(mCurrentVideoUrl)
                                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                                .setContentType("video/mp4")
                                .setMetadata(metadata).build();

                        Log.e(TAG, "should show!");
                        if (mApiClient != null && mApiClient.isConnected()) {
                            if (mMediaPlayer != null) {
                                playMedia(mSelectedMedia);
                            }

                        }
                    }
                });
        if (mSiteUrl != null) {
            final String title = mCurrentView.getTitle();
            listDialog.setDialogTitile(title + "[" + mSiteUrl + "]");
        } else {
            listDialog.setDialogTitile(getResources().getString(
                    R.string.custom_dialog_list_title_str));
        }

        videoList.clear();

        videoList.addAll(videoUrls.keySet());

        listDialog.setListData(videoList);
        listDialog.show();
    }

    public String getCurrentResolution() {
        return mVideoResolutionTextView.getText().toString();
    }
}
