package com.team.lottery.reports.repository;

import com.team.lottery.reports.dto.DrawReportEntry;

import java.util.List;

public interface ReportRepository {

    /**
     * Возвращает строки отчёта по всем завершённым (status = COMPLETED) тиражам.
     * Сортировка — по дате розыгрыша по убыванию (самые новые сверху).
     */
    List<DrawReportEntry> getCompletedDrawsReport();
}
