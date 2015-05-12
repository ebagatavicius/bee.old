package com.butent.bee.shared.modules.projects;

public interface LocalizableProjectMessages {
  String newProjectCreated(Long projectId);

  String projectCanCreateTaskOwner(Long taskId);

  String taskAssignedToProject(Long taskId, Long projectId);
}
