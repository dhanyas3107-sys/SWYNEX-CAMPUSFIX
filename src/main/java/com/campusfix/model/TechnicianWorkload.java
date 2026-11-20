package com.campusfix.model;

/** A technician together with how many tasks they currently hold and have completed. */
public record TechnicianWorkload(Technician technician, int activeAssignments, int completedTasks) { }
