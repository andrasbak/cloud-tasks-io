/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.cloudtasks;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.cloudtasks.TaskApplication.TaskListener;
import com.cloudtasks.shared.CloudTasksRequestFactory;
import com.cloudtasks.shared.TaskChange;
import com.cloudtasks.shared.TaskProxy;
import com.cloudtasks.shared.TaskRequest;

/**
 * Main activity - requests "Hello, World" messages from the server and provides
 * a menu item to invoke the accounts activity.
 */
public class CloudTasksActivity extends Activity implements OnItemClickListener {
    /**
     * Tag for logging.
     */
    private static final String TAG = "CloudTasksActivity";

    /**
     * The current context.
     */
    private Context mContext = this;

    /**
     * A {@link BroadcastReceiver} to receive the response from a register or
     * unregister request, and to update the UI.
     */
    private final BroadcastReceiver mUpdateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(DeviceRegistrar.STATUS_EXTRA,
                    DeviceRegistrar.ERROR_STATUS);
            String message = null;
            if (status == DeviceRegistrar.REGISTERED_STATUS) {
                message = getResources().getString(R.string.registration_succeeded);
            } else if (status == DeviceRegistrar.UNREGISTERED_STATUS) {
                message = getResources().getString(R.string.unregistration_succeeded);
            } else {
                message = getResources().getString(R.string.registration_error);
            }

            // Display a notification
            SharedPreferences prefs = Util.getSharedPreferences(mContext);
            String accountName = prefs.getString(Util.ACCOUNT_NAME, "Unknown");
            Util.generateNotification(mContext, String.format(message, accountName));
        }
    };
    
    private final static int NEW_TASK_REQUEST = 1;
    private ListView listView;
    private View progressBar;
    private TaskAdapter adapter;
    private AsyncFetchTask task;


    /**
     * Begins the activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);


        setContentView(R.layout.tasklist);

        listView = (ListView) findViewById(R.id.list);
        progressBar = findViewById(R.id.title_refresh_progress);

        // get the task application to store the adapter which will act as the task storage
        // for this demo.
        TaskApplication taskApplication = (TaskApplication) getApplication();
        adapter = taskApplication.getAdapter(this);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);

        // Register a receiver to provide register/unregister notifications
        registerReceiver(mUpdateUIReceiver, new IntentFilter(Util.UPDATE_UI_INTENT));
    }

    /**
     * Shuts down the activity.
     */
    @Override
    public void onDestroy() {
        unregisterReceiver(mUpdateUIReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        // Invoke the Register activity
        menu.getItem(0).setIntent(new Intent(this, AccountsActivity.class));
        return true;
    }





    @Override
    protected void onStart() {
        super.onStart();

        // only fetch task on start if the registration has happened.
        SharedPreferences prefs = Util.getSharedPreferences(mContext);
        String deviceRegistrationID = prefs.getString(Util.DEVICE_REGISTRATION_ID, null);
        if (deviceRegistrationID != null) {
        	fetchTasks(-1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TaskApplication taskApplication = (TaskApplication) getApplication();
        taskApplication.setTaskListener(new TaskListener() {
            public void onTaskUpdated(final String message, final long id) {
            	runOnUiThread(new Runnable() {
                    public void run() {
                        if (TaskChange.UPDATE.equals(message)) {
                            fetchTasks(id);
                        }
                    }
                });
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        TaskApplication taskApplication = (TaskApplication) getApplication();
        taskApplication.setTaskListener(null);
    }


    public void fetchTasks(long id) {
        progressBar.setVisibility(View.VISIBLE);
        if (task != null) {
            task.cancel(true);
        }
        task = new AsyncFetchTask(this);
        task.execute(id);
    }

    public void setTasks(List<TaskProxy> tasks) {
        progressBar.setVisibility(View.GONE);
        adapter.setTasks(tasks);
        adapter.notifyDataSetChanged();
    }

    public void addTasks(List<TaskProxy> tasks) {
        progressBar.setVisibility(View.GONE);
        adapter.addTasks(tasks);
        adapter.notifyDataSetChanged();
    }

    public void onAddClick(View view) {
        Intent intent = new Intent(this, AddTaskActivity.class);
        startActivityForResult(intent, NEW_TASK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case NEW_TASK_REQUEST:
            if (resultCode == Activity.RESULT_OK) {
                final String taskName = data.getStringExtra("task");
                final String taskDetails = data.getStringExtra("details");

                Calendar c = Calendar.getInstance();
                c.set(data.getIntExtra("year", 2011),
                        data.getIntExtra("month", 12),
                        data.getIntExtra("day", 31));
                final Date dueDate = c.getTime();

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... arg0) {
                        CloudTasksRequestFactory factory = (CloudTasksRequestFactory) Util
                                .getRequestFactory(CloudTasksActivity.this,
                                        CloudTasksRequestFactory.class);
                        TaskRequest request = factory.taskRequest();

                        TaskProxy task = request.create(TaskProxy.class);
                        task.setName(taskName);
                        task.setNote(taskDetails);
                        task.setDueDate(dueDate);

                        request.updateTask(task).fire();

                        return null;
                    }

                }.execute();
            }
            break;
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, ViewTaskActivity.class);
        intent.putExtra("position", position);
        startActivity(intent);
    }


}
