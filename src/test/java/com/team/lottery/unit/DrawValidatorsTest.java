package com.team.lottery.unit;

import com.team.lottery.common.errors.ValidationException;
import com.team.lottery.draws.dto.CreateDrawRequest;
import com.team.lottery.draws.validation.DrawValidators;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DrawValidatorsTest {

    private static CreateDrawRequest valid() {
        return new CreateDrawRequest(
                "Test draw",
                OffsetDateTime.now().plusDays(1),
                100
        );
    }

    private static CreateDrawRequest withTitle(String title) {
        CreateDrawRequest base = valid();
        return new CreateDrawRequest(title, base.endDate(), base.totalTickets());
    }

    private static CreateDrawRequest withEndDate(OffsetDateTime endDate) {
        CreateDrawRequest base = valid();
        return new CreateDrawRequest(base.title(), endDate, base.totalTickets());
    }

    private static CreateDrawRequest withTotalTickets(Integer totalTickets) {
        CreateDrawRequest base = valid();
        return new CreateDrawRequest(base.title(), base.endDate(), totalTickets);
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
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(withTitle("   ")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title")
                .hasMessageContaining("blank");
    }

    @Test
    void throwsForNullTitle() {
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(withTitle(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("title");
    }

    @Test
    void throwsForNullEndDate() {
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(withEndDate(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("endDate");
    }

    @Test
    void throwsForNullTotalTickets() {
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(withTotalTickets(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("totalTickets");
    }

    @Test
    void throwsForZeroTotalTickets() {
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(withTotalTickets(0)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("totalTickets")
                .hasMessageContaining("positive");
    }

    @Test
    void throwsForNegativeTotalTickets() {
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(withTotalTickets(-5)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("totalTickets");
    }

    @Test
    void throwsForEndDateInPast() {
        assertThatThrownBy(() -> DrawValidators.createDrawRequest(withEndDate(OffsetDateTime.now().minusDays(1))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("endDate")
                .hasMessageContaining("past");
    }
}
