package com.limitless.hook_plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by Nick on 2018/9/30
 *
 * @author Nick
 * 登录界面
 */
public class LoginActivity extends Activity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        findViewById(R.id.btn_login_success).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.isLogin = true;
                Intent intent = new Intent(getBaseContext(), ShowActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
