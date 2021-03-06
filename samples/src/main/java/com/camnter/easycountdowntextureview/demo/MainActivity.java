package com.camnter.easycountdowntextureview.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.camnter.easycountdowntextureview.demo.adapter.MainAdapter;
import com.camnter.easyrecyclerview.holder.EasyRecyclerViewHolder;
import com.camnter.easyrecyclerview.widget.EasyRecyclerView;
import com.camnter.easyrecyclerview.widget.decorator.EasyDividerItemDecoration;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EasyRecyclerView mainRecyclerView;
    private MainAdapter mainAdapter;
    private ArrayList<Class> classes;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.initViews();
        this.initData();
        this.initListeners();
    }


    private void initViews() {
        this.mainRecyclerView = (EasyRecyclerView) this.findViewById(R.id.main_rv);
        EasyDividerItemDecoration decoration = new EasyDividerItemDecoration(this,
                EasyDividerItemDecoration.VERTICAL_LIST);
        decoration.bottomDivider = true;
        this.mainRecyclerView.addItemDecoration(decoration);
    }


    private void initData() {
        this.classes = new ArrayList<>();
        this.classes.add(ShowActivity.class);
        this.classes.add(SettingActivity.class);
        this.classes.add(CompletedActivity.class);

        this.mainAdapter = new MainAdapter();
        this.mainAdapter.setList(this.classes);
        this.mainRecyclerView.setAdapter(this.mainAdapter);
    }


    private void initListeners() {
        this.mainAdapter.setOnItemClickListener(new EasyRecyclerViewHolder.OnItemClickListener() {
            @Override public void onItemClick(View view, int i) {
                Class c = MainActivity.this.classes.get(i);
                MainActivity.this.startActivity(new Intent(MainActivity.this, c));
            }
        });
    }

}
