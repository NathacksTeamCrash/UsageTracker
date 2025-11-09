package com.example.usagetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.usagetracker.models.User;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {
    private List<User> usersList;

    public LeaderboardAdapter(List<User> usersList) {
        this.usersList = usersList;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        User user = usersList.get(position);
        int rank = position + 1;

        holder.rankTextView.setText(String.valueOf(rank));
        holder.nameTextView.setText(user.getName());
        holder.pointsTextView.setText(String.valueOf(user.getEcoPoints()));

        // Highlight top 3
        if (rank == 1) {
            holder.rankTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark, null));
        } else if (rank == 2) {
            holder.rankTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.darker_gray, null));
        } else if (rank == 3) {
            holder.rankTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_orange_light, null));
        }
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView rankTextView, nameTextView, pointsTextView;

        LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            rankTextView = itemView.findViewById(R.id.rankTextView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            pointsTextView = itemView.findViewById(R.id.pointsTextView);
        }
    }
}

