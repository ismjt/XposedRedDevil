package com.hxs.xposedreddevil.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hxs.xposedreddevil.R;
import com.hxs.xposedreddevil.adapter.FilterAdapter;
import com.hxs.xposedreddevil.contentprovider.PropertiesUtils;
import com.hxs.xposedreddevil.model.FilterBean;
import com.hxs.xposedreddevil.model.FilterSaveBean;
import com.hxs.xposedreddevil.service.GroupChatService;
import com.hxs.xposedreddevil.utils.MessageEvent;
import com.hxs.xposedreddevil.weight.LoadingDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.hxs.xposedreddevil.ui.MainActivity.RED_FILE;

public class SelectFilterActivity extends AppCompatActivity implements FilterAdapter.onItemClickListener {

    @BindView(R.id.iv_class_back)
    ImageView ivClassBack;
    @BindView(R.id.tv_class_name)
    TextView tvClassName;
    @BindView(R.id.tv_class_add)
    TextView tvClassAdd;
    @BindView(R.id.rl_select)
    RecyclerView rlSelect;

    LoadingDialog loadingDialog;

    FilterBean filterBean;
    FilterSaveBean bean;
    List<FilterBean> beanList = new ArrayList<>();
    FilterAdapter adapter;

    Gson gson = new Gson();
    JsonParser parser = new JsonParser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_filter);
        ButterKnife.bind(this);
        DataInit();
    }

    private void DataInit() {
        EventBus.getDefault().register(this);
        loadingDialog = new LoadingDialog(this);
        tvClassName.setText("选择过滤的群聊");
        startService(new Intent(this, GroupChatService.class));
        loadingDialog.show();
        rlSelect.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FilterAdapter(beanList, this);
        rlSelect.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getMsg(Map<String, String> chatRoommap) {
        loadingDialog.dismiss();
        for (Map.Entry<String, String> entry : chatRoommap.entrySet()) {
            filterBean = new FilterBean();
            filterBean.setName(entry.getKey());
            filterBean.setDisplayname(entry.getValue());
            filterBean.setCheck(false);
            beanList.add(filterBean);
        }
        if(!PropertiesUtils.getValue(RED_FILE,"selectfilter","").equals("")){
            List<FilterSaveBean> list = new ArrayList<>();
            JsonArray jsonArray = parser.parse(PropertiesUtils
                    .getValue(RED_FILE,"selectfilter","")).getAsJsonArray();
            for (JsonElement user : jsonArray) {
                bean = new FilterSaveBean();
                //使用GSON，直接转成Bean对象
                bean = gson.fromJson(user, FilterSaveBean.class);
                list.add(bean);
            }
            for (int i = 0; i < beanList.size(); i++) {
                for (int j = 0; j < list.size(); j++) {
                    if(beanList.get(i).getName().equals(list.get(j).getName())){
                        beanList.get(i).setCheck(true);
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getError(MessageEvent msg) {
        if (msg.getMessage().equals("error")) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        stopService(new Intent(this, GroupChatService.class));
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    public void itemClickListener(View v, int i) {
        if (beanList.get(i).isCheck()) {
            beanList.get(i).setCheck(false);
        } else {
            beanList.get(i).setCheck(true);
        }
        adapter.notifyDataSetChanged();
    }

    @OnClick({R.id.iv_class_back, R.id.tv_class_add})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_class_back:
                finish();
                break;
            case R.id.tv_class_add:
                List<FilterSaveBean> list = new ArrayList<>();
                for (int i = 0; i < beanList.size(); i++) {
                    if(beanList.get(i).isCheck()){
                        bean = new FilterSaveBean();
                        bean.setName(beanList.get(i).getName());
                        bean.setDisplayname(beanList.get(i).getDisplayname());
                        list.add(bean);
                    }
                }
                PropertiesUtils.putValue(RED_FILE, "selectfilter", gson.toJson(list));
                Toast.makeText(this, "添加完成", Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
    }
}
