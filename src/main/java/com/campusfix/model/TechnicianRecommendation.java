package com.campusfix.model;

/** One ranked suggestion produced by the rule-based technician recommender. */
public record TechnicianRecommendation(Technician technician, int activeAssignments,
                                       boolean specializationMatch, String reason) { }
