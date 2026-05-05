package com.team.lottery.draws.validation;

import com.team.lottery.common.errors.ValidationException;
import com.team.lottery.common.validation.Validators;
import com.team.lottery.draws.dto.CreateDrawRequest;

import java.time.OffsetDateTime;

public final class DrawValidators {

    private DrawValidators() {
    }

    public static void createDrawRequest(CreateDrawRequest req) {
        Validators.notNull(req, "request");
        Validators.notBlank(req.title(), "title");
        Validators.notNull(req.endDate(), "endDate");
        Validators.notNull(req.totalTickets(), "totalTickets");
        Validators.positive(req.totalTickets(), "totalTickets");
        endDateInFuture(req.endDate());
    }

    private static void endDateInFuture(OffsetDateTime endDate) {
        if (endDate.isBefore(OffsetDateTime.now())) {
            throw new ValidationException("endDate must not be in the past");
        }
    }
}
