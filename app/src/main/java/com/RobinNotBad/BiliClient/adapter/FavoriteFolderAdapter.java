package com.RobinNotBad.BiliClient.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RobinNotBad.BiliClient.R;
import com.RobinNotBad.BiliClient.activity.user.favorite.FavoriteVideoListActivity;
import com.RobinNotBad.BiliClient.model.FavoriteFolder;
import com.RobinNotBad.BiliClient.util.ToolsUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

//收藏夹Adapter

public class FavoriteFolderAdapter extends RecyclerView.Adapter<FavoriteFolderAdapter.FavoriteHolder> {

    Context context;
    ArrayList<FavoriteFolder> folderList;
    long mid;

    public FavoriteFolderAdapter(Context context, ArrayList<FavoriteFolder> folderList, long mid) {
        this.context = context;
        this.folderList = folderList;
        this.mid = mid;
    }

    @NonNull
    @Override
    public FavoriteHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(this.context).inflate(R.layout.cell_favorite_folder_list,parent,false);
        return new FavoriteHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull FavoriteHolder holder, int position) {
        holder.name.setText(ToolsUtil.htmlToString(folderList.get(position).name));
        int _count = folderList.get(position).videoCount;
        int _max = folderList.get(position).maxCount;
        // v3 接口不再返回收藏夹上限；没有上限时只显示数量，不要显示 "12/0"
        holder.count.setText(_max > 0 ? (_count + "/" + _max) : (_count + " 个视频"));
        String _cover = folderList.get(position).cover;
        if (_cover != null && !_cover.isEmpty()) {
            Glide.with(this.context).load(_cover + "@20q.webp")
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(ToolsUtil.dp2px(5, context))))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(holder.cover);
        } else {
            holder.cover.setImageDrawable(null);   // v3 接口不返回封面，留空白而不是加载失败
        }
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(context, FavoriteVideoListActivity.class);
            intent.putExtra("fid",folderList.get(position).id);
            intent.putExtra("mid",mid);
            intent.putExtra("name",folderList.get(position).name);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    public static class FavoriteHolder extends RecyclerView.ViewHolder {
        TextView name,count;
        ImageView cover;

        public FavoriteHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.title);
            count = itemView.findViewById(R.id.itemCount);
            cover = itemView.findViewById(R.id.cover);
        }
    }
}
