package com.example.usagetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import android.view.View;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalViewHolder> {
    private List<String> goalsList;

    public GoalsAdapter(List<String> goalsList) {
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
        String goal = goalsList.get(position);
        holder.typeTextView.setText(goal);
    }

    @Override
    public int getItemCount() {
        return goalsList.size();
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView typeTextView;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
        }
    }
}

