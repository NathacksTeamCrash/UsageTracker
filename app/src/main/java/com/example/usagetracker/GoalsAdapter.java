package com.example.usagetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.usagetracker.models.Goal;

import java.util.List;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalViewHolder> {
    private List<Goal> goalsList;

    public GoalsAdapter(List<Goal> goalsList) {
        this.goalsList = goalsList;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        Goal goal = goalsList.get(position);
        holder.activityNameTextView.setText(goal.getActivityName());
        holder.targetTextView.setText("Target: " + goal.getTargetLimit() + " " + goal.getUnit());
        holder.typeTextView.setText(goal.getType());
        holder.frequencyTextView.setText(goal.getFrequency());
    }

    @Override
    public int getItemCount() {
        return goalsList.size();
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView activityNameTextView, targetTextView, typeTextView, frequencyTextView;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            activityNameTextView = itemView.findViewById(R.id.activityNameTextView);
            targetTextView = itemView.findViewById(R.id.targetTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            frequencyTextView = itemView.findViewById(R.id.frequencyTextView);
        }
    }
}

