package com.team.lottery.unit;

import com.team.lottery.common.errors.ValidationException;
import com.team.lottery.draws.dto.CreateDrawRequest;
import com.team.lottery.draws.validation.DrawValidators;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DrawValidatorsTest {

    private CreateDrawRequest valid() {
        return new CreateDrawRequest(
                "Test draw",
                OffsetDateTime.now().plusDays(1),
                100
        );
    }

    @Test
    void passesForValidRequest() {
        assertThatCode(() -> DrawValidators.createDrawRequest(valid()))
                .doesNotThrowAnyException();
    }

    @Test
    void throwsForNullRequest() {
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("request");
    }

    @Test
    void throwsForBlankTitle() {
        CreateDrawRequest req = valid();
        req.setTitle("   ");
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title")
                .hasMessageContaining("blank");
    }

    @Test
    void throwsForNullTitle() {
        CreateDrawRequest req = valid();
        req.setTitle(null);
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title");
    }

    @Test
    void throwsForNullEndDate() {
        CreateDrawRequest req = valid();
        req.setEndDate(null);
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("endDate");
    }

    @Test
    void throwsForNullTotalTickets() {
        CreateDrawRequest req = valid();
        req.setTotalTickets(null);
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("totalTickets");
    }

    @Test
    void throwsForZeroTotalTickets() {
        CreateDrawRequest req = valid();
        req.setTotalTickets(0);
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("totalTickets")
                .hasMessageContaining("positive");
    }

    @Test
    void throwsForNegativeTotalTickets() {
        CreateDrawRequest req = valid();
        req.setTotalTickets(-5);
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("totalTickets");
    }

    @Test
    void throwsForEndDateInPast() {
        CreateDrawRequest req = valid();
        req.setEndDate(OffsetDateTime.now().minusDays(1));
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("endDate")
                .hasMessageContaining("past");
    }
}
