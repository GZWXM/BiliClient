package com.RobinNotBad.BiliClient.activity.user.favorite;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.RobinNotBad.BiliClient.activity.base.RefreshListActivity;
import com.RobinNotBad.BiliClient.adapter.video.VideoCardAdapter;
import com.RobinNotBad.BiliClient.api.FavoriteApi;
import com.RobinNotBad.BiliClient.api.PlayerApi;
import com.RobinNotBad.BiliClient.api.VideoInfoApi;
import com.RobinNotBad.BiliClient.model.PlayerData;
import com.RobinNotBad.BiliClient.model.VideoCard;
import com.RobinNotBad.BiliClient.model.VideoInfo;
import com.RobinNotBad.BiliClient.util.CenterThreadPool;
import com.RobinNotBad.BiliClient.util.MsgUtil;
import com.RobinNotBad.BiliClient.util.SharedPreferencesUtil;
import com.RobinNotBad.BiliClient.util.ToolsUtil;

import java.util.ArrayList;

//收藏夹内
//2023-08-08
//2024-05-01

public class FavoriteVideoListActivity extends RefreshListActivity {

    private long mid;
    private long fid;
    private String folderName = "";
    private ArrayList<VideoCard> videoList;
    private VideoCardAdapter videoCardAdapter;

    private int longClickPosition = -1;
    private long longClickTimestamp;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mid = intent.getLongExtra("mid", 0);
        fid = intent.getLongExtra("fid", 0);
        String name = intent.getStringExtra("name");
        folderName = name == null ? "" : name;

        setPageName(name);
        initPlayAllButton();

        videoList = new ArrayList<>();

        CenterThreadPool.run(() -> {
            try {
                int result = FavoriteApi.getFolderVideos(mid, fid, page, videoList);
                if (result != -1) {
                    videoCardAdapter = new VideoCardAdapter(this, videoList);

                    videoCardAdapter.setOnLongClickListener(position -> {
                        long timestamp = System.currentTimeMillis();
                        if (longClickPosition == position && timestamp - longClickTimestamp < 4000) {
                            CenterThreadPool.run(() -> {
                                try {
                                    int delResult = FavoriteApi.deleteFavorite(videoList.get(position).aid, fid);
                                    longClickPosition = -1;
                                    if (delResult == 0) runOnUiThread(() -> {
                                        MsgUtil.showMsg("删除成功");
                                        videoList.remove(position);
                                        videoCardAdapter.notifyItemRemoved(position);
                                        videoCardAdapter.notifyItemRangeChanged(position, videoList.size() - position);
                                    });
                                    else
                                        runOnUiThread(() -> MsgUtil.showMsg("删除失败，错误码：" + delResult));
                                } catch (Exception e) {
                                    report(e);
                                }
                            });
                        } else {
                            longClickPosition = position;
                            longClickTimestamp = timestamp;
                            MsgUtil.showMsg("再次长按删除");
                        }
                    });

                    setOnLoadMoreListener(this::continueLoading);
                    setAdapter(videoCardAdapter);
                    setRefreshing(false);

                    if (result == 1) {
                        Log.e("debug", "到底了");
                        setBottom(true);
                    }
                }

            } catch (Exception e) {
                loadFail(e);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void continueLoading(int page) {
        CenterThreadPool.run(() -> {
            try {
                int lastSize = videoList.size();
                int result = FavoriteApi.getFolderVideos(mid, fid, page, videoList);
                if (result != -1) {
                    Log.e("debug", "下一页");
                    runOnUiThread(() -> videoCardAdapter.notifyItemRangeInserted(lastSize, videoList.size() - lastSize));
                    if (result == 1) {
                        Log.e("debug", "到底了");
                        setBottom(true);
                    }
                }
                setRefreshing(false);
            } catch (Exception e) {
                loadFail(e);
            }
        });
    }

    //右下角「连播」按钮：从列表第一个视频开始，一个播完自动播下一个
    private void initPlayAllButton() {
        ViewGroup root = findViewById(android.R.id.content);
        if (root == null) return;

        TextView button = new TextView(this);
        button.setText("▶ 连播");
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        int padH = ToolsUtil.dp2px(14), padV = ToolsUtil.dp2px(8);
        button.setPadding(padH, padV, padH, padV);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCCFB8787);
        bg.setCornerRadius(ToolsUtil.dp2px(18));
        button.setBackground(bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            button.setElevation(ToolsUtil.dp2px(4));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.setMargins(0, 0, ToolsUtil.dp2px(16), ToolsUtil.dp2px(16));
        button.setLayoutParams(params);
        button.setOnClickListener(view -> startPlayAll());

        root.addView(button);
    }

    private void startPlayAll() {
        if (videoList == null || videoList.isEmpty()) {
            MsgUtil.showMsg("列表还没加载好呢");
            return;
        }
        if (!"terminalPlayer".equals(SharedPreferencesUtil.getString("player", ""))) {
            MsgUtil.showMsgLong("连播要用内置播放器才能拿到播放结束事件\n请到 设置→播放器 选「哔哩终端内置播放器」");
            return;
        }

        int firstIndex = -1;
        for (int i = 0; i < videoList.size(); i++) {
            VideoCard card = videoList.get(i);
            if (card != null && card.aid > 0 && "video".equals(card.type)) {
                firstIndex = i;
                break;
            }
        }
        if (firstIndex < 0) {
            MsgUtil.showMsg("这个收藏夹里没有能播的视频");
            return;
        }

        final int startIndex = firstIndex;
        final ArrayList<VideoCard> snapshot = new ArrayList<>(videoList);
        final int startPage = page;
        final boolean startEnd = bottom;

        setRefreshing(true);
        MsgUtil.showMsg("正在准备连播…");

        CenterThreadPool.run(() -> {
            try {
                VideoInfo info = VideoInfoApi.getVideoInfo(snapshot.get(startIndex).aid);
                if (info == null || info.cids == null || info.cids.isEmpty()) {
                    runOnUiThread(() -> {
                        setRefreshing(false);
                        MsgUtil.showMsg("第一个视频获取失败");
                    });
                    return;
                }

                PlayerData playerData = info.toPlayerData(0);
                playerData.qn = SharedPreferencesUtil.getInt("play_qn", 16);
                playerData.progress = 0;
                PlayerApi.getVideo(playerData, false);

                Intent intent = PlayerApi.jumpToPlayer(playerData);
                intent.putParcelableArrayListExtra("fav_playlist", snapshot);
                intent.putExtra("fav_index", startIndex);
                intent.putExtra("fav_mid", mid);
                intent.putExtra("fav_fid", fid);
                intent.putExtra("fav_name", folderName);
                intent.putExtra("fav_page", startPage);
                intent.putExtra("fav_end", startEnd);

                runOnUiThread(() -> {
                    setRefreshing(false);
                    startActivity(intent);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setRefreshing(false);
                    report(e);
                });
            }
        });
    }
}