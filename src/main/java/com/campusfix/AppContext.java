package com.campusfix;

import com.campusfix.dao.*;
import com.campusfix.service.*;

/** Creates the DAOs and services once and wires them together (manual dependency injection). */
public class AppContext {

    public final UserService userService;
    public final AuthenticationService authenticationService;
    public final ComplaintService complaintService;
    public final TechnicianService technicianService;
    public final ReportService reportService;

    public AppContext() {
        UserDAO userDAO = new UserDAO();
        ComplaintDAO complaintDAO = new ComplaintDAO();
        ComplaintHistoryDAO historyDAO = new ComplaintHistoryDAO();
        TechnicianDAO technicianDAO = new TechnicianDAO();
        ReportDAO reportDAO = new ReportDAO();

        this.userService = new UserService(userDAO);
        this.authenticationService = new AuthenticationService(userDAO);
        this.complaintService = new ComplaintService(complaintDAO, historyDAO, technicianDAO);
        this.technicianService = new TechnicianService(technicianDAO, userDAO, complaintDAO);
        this.reportService = new ReportService(reportDAO, technicianDAO);
    }
}
