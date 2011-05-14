package com.cloudtasks.shared;

import java.util.List;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.ServiceName;

@ServiceName("com.cloudtasks.server.CloudTasksService")
public interface TaskRequest extends RequestContext {

	Request<TaskProxy> createTask();

	Request<TaskProxy> readTask(Long id);

	Request<TaskProxy> updateTask(TaskProxy task);

	Request<Void> deleteTask(TaskProxy task);

	Request<List<TaskProxy>> queryTasks();

}
