package com.team.lottery.reports.controller;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.lottery.common.errors.ValidationException;
import com.team.lottery.reports.dto.DrawReportEntry;
import com.team.lottery.reports.repository.ReportRepository;
import com.team.lottery.users.service.AuthService;
import io.javalin.config.RoutesConfig;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportController {

    private static final DateTimeFormatter FILENAME_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(ZoneOffset.UTC);

    private static final CsvMapper CSV_MAPPER = buildCsvMapper();

    private static final CsvSchema COMPLETED_DRAWS_SCHEMA = CsvSchema.builder()
            .addColumn("drawId")
            .addColumn("title")
            .addColumn("createdAt")
            .addColumn("endDate")
            .addColumn("totalTickets")
            .addColumn("soldTickets")
            .addColumn("winnerTicketNumber")
            .addColumn("winnerUserId")
            .addColumn("winnerLogin")
            .addColumn("drawnAt")
            .addColumn("createdByAdminId")
            .addColumn("createdByAdminLogin")
            .build()
            .withHeader();

    private final ReportRepository reportRepository;
    private final AuthService auth;

    public ReportController(ReportRepository reportRepository, AuthService auth) {
        this.reportRepository = reportRepository;
        this.auth = auth;
    }

    public void registerRoutes(RoutesConfig routes) {
        routes.get("/admin/reports/draws/completed", ctx -> {
            auth.requireAdmin(ctx);

            String format = parseFormat(ctx.queryParam("format"));

            List<DrawReportEntry> entries = reportRepository.getCompletedDrawsReport();

            if ("csv".equals(format)) {
                String csv = CSV_MAPPER.writer(COMPLETED_DRAWS_SCHEMA).writeValueAsString(entries);
                String filename = "completed-draws-"
                        + FILENAME_TIMESTAMP.format(OffsetDateTime.now())
                        + ".csv";

                ctx.contentType("text/csv; charset=UTF-8");
                ctx.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                ctx.result(csv);
            } else {
                ctx.json(entries);
            }
        });
    }

    private static String parseFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            return "json";
        }
        String normalized = raw.trim().toLowerCase();
        if (!"json".equals(normalized) && !"csv".equals(normalized)) {
            throw new ValidationException("format must be 'csv' or 'json'");
        }
        return normalized;
    }

    private static CsvMapper buildCsvMapper() {
        CsvMapper mapper = new CsvMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
