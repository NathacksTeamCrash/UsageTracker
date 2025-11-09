package com.example.usagetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.usagetracker.models.Goal;

import java.util.List;

public class ActivityProgressAdapter extends RecyclerView.Adapter<ActivityProgressAdapter.ProgressViewHolder> {
    private List<Goal> goalsList;
    private java.util.Map<String, Double> currentUsageMap; // goalId -> current usage

    public ActivityProgressAdapter(List<Goal> goalsList) {
        this.goalsList = goalsList;
        this.currentUsageMap = new java.util.HashMap<>();
    }

    public void setCurrentUsage(String goalId, double usage) {
        currentUsageMap.put(goalId, usage);
        notifyDataSetChanged();
    }

    public void setCurrentUsageMap(java.util.Map<String, Double> usageMap) {
        this.currentUsageMap = usageMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_progress, parent, false);
        return new ProgressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, int position) {
        Goal goal = goalsList.get(position);
        double currentUsage = currentUsageMap.getOrDefault(goal.getGoalId(), 0.0);
        double target = goal.getTargetLimit();
        double progress = target > 0 ? Math.min((currentUsage / target) * 100, 100) : 0;

        holder.activityNameTextView.setText(goal.getActivityName());
        holder.targetTextView.setText(String.format("Target: %.1f %s", target, goal.getUnit()));
        holder.currentTextView.setText(String.format("Current: %.1f %s", currentUsage, goal.getUnit()));
        holder.progressBar.setProgress((int) progress);

        // Color coding: green if under target, red if over
        if (currentUsage <= target) {
            holder.progressBar.getProgressDrawable().setColorFilter(
                    holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark, null),
                    android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            holder.progressBar.getProgressDrawable().setColorFilter(
                    holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark, null),
                    android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public int getItemCount() {
        return goalsList.size();
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        TextView activityNameTextView, targetTextView, currentTextView;
        ProgressBar progressBar;

        ProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            activityNameTextView = itemView.findViewById(R.id.activityNameTextView);
            targetTextView = itemView.findViewById(R.id.targetTextView);
            currentTextView = itemView.findViewById(R.id.currentTextView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}

