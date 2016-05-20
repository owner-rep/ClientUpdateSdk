/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.hellolibs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.uddream.bs.BSUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        setContentView(tv);

        new AsyncTask<String, String, String>() {

            @Override
            protected String doInBackground(String... params) {
                String base = Environment.getExternalStorageDirectory() + File.separator;
                Log.e("sd", base);
                String oldFile = base + "bsdiff/app.apk";
                String newFile = base + "bsdiff/new.apk";
                String patchFile = base + "bsdiff/patch.apk";
                int ret = BSUtil.bspatch(oldFile, newFile, patchFile);
                String md5 = BSUtil.md5sum(newFile);
                return String.valueOf("md5:" + md5 + "\nsize:" + ret);
            }

            @Override
            protected void onPostExecute(String s) {

                Toast.makeText(MainActivity.this, "合并完成，状态码：" + s, Toast.LENGTH_LONG).show();
            }
        }.execute("");
    }
}
