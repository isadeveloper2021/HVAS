package com.example.hvas.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hvas.databinding.ItemDataBinding;

import java.util.List;

public class ListDataAdapter extends RecyclerView.Adapter<ListDataAdapter.ViewHolder> {

    private final List<String> dataList;
    private final ItemListener itemListener;

    public ListDataAdapter(List<String> dataList, ItemListener itemListener) {
        this.dataList = dataList;
        this.itemListener = itemListener;
    }

    @NonNull
    @Override
    public ListDataAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDataBinding itemDataBinding = ItemDataBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(itemDataBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ListDataAdapter.ViewHolder holder, int position) {
        holder.binding.tvData.setText(dataList.get(position));

//        holder.binding.tvData.setOnClickListener(view -> itemListener.onItemClicked(dataList.get(holder.getAdapterPosition()), holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ItemDataBinding binding;

        public ViewHolder(@NonNull ItemDataBinding binding) {
            super(binding.getRoot());

            this.binding = binding;

        }
    }

    public interface ItemListener {
        void onItemClicked(String data, int index);
    }
}
