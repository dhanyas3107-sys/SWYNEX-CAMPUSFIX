package com.campusfix.model;

import java.util.List;
import java.util.Map;

/** Snapshot of everything shown on the administrator's report screen. */
public record SystemReport(int total, int resolved, int pending, int inProgress,
                           Map<String, Integer> byStatus,
                           Map<String, Integer> byCategory,
                           Map<String, Integer> byPriority,
                           List<TechnicianWorkload> technicianWorkloads,
                           ResolutionStats resolutionStats) { }
