package com.cloudtasks.shared;

import com.google.web.bindery.requestfactory.shared.RequestFactory;


public interface CloudTasksRequestFactory extends RequestFactory {

	TaskRequest taskRequest();

}
