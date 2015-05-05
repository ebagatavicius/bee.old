package com.butent.bee.shared.modules.projects;

public interface LocalizableProjectMessages {
  String newProjectCreated(Long projectId);

  String taskAssignedToProject(Long taskId, Long projectId);
}
