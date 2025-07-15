package com.example.omnicontrol.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.omnicontrol.databinding.ItemConnectionHistoryBinding;
import com.example.omnicontrol.models.ConnectionRecord;
import java.util.ArrayList;
import java.util.List;

public class ConnectionHistoryAdapter extends RecyclerView.Adapter<ConnectionHistoryAdapter.ViewHolder> {
    
    private List<ConnectionRecord> records = new ArrayList<>();
    
    public void setRecords(List<ConnectionRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConnectionHistoryBinding binding = ItemConnectionHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(records.get(position));
    }
    
    @Override
    public int getItemCount() {
        return records.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemConnectionHistoryBinding binding;
        
        public ViewHolder(ItemConnectionHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        public void bind(ConnectionRecord record) {
            binding.tvIpAddress.setText(record.getIpAddress());
            binding.tvConnectionTime.setText(record.getConnectionTime());
            binding.tvStatus.setText(record.getStatus());
            binding.tvDuration.setText(record.getDuration());
        }
    }
}
