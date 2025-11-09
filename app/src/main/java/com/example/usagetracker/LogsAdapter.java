package com.example.usagetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.usagetracker.models.UsageLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogViewHolder> {
    private List<UsageLog> logsList;

    public LogsAdapter(List<UsageLog> logsList) {
        this.logsList = logsList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        UsageLog log = logsList.get(position);
        
        holder.activityNameTextView.setText(log.getActivityName());
        holder.usageAmountTextView.setText(String.format("%.2f", log.getUsageAmount()));
        holder.typeTextView.setText(log.getType());
        
        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.dateTextView.setText(sdf.format(new Date(log.getTimestamp())));
        
        // Show goal status
        if (log.isMetGoal()) {
            holder.statusTextView.setText("✓ Goal Met");
            holder.statusTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.statusTextView.setText("✗ Over Goal");
            holder.statusTextView.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        }
        
        // Show points earned
        if (log.getEcoPointsEarned() > 0) {
            holder.pointsTextView.setText("+" + log.getEcoPointsEarned() + " pts");
            holder.pointsTextView.setVisibility(View.VISIBLE);
        } else {
            holder.pointsTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return logsList.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView activityNameTextView, usageAmountTextView, typeTextView, 
                 dateTextView, statusTextView, pointsTextView;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            activityNameTextView = itemView.findViewById(R.id.activityNameTextView);
            usageAmountTextView = itemView.findViewById(R.id.usageAmountTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            pointsTextView = itemView.findViewById(R.id.pointsTextView);
        }
    }
}

