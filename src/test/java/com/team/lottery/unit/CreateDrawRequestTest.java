package com.team.lottery.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.lottery.draws.dto.CreateDrawRequest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CreateDrawRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void shouldDeserializeFromJson() throws Exception {
        String json = """
                {
                    "title": "Super Prize",
                    "endDate": "2024-10-15T10:00:00Z",
                    "totalTickets": 100
                }
                """;

        CreateDrawRequest result = objectMapper.readValue(json, CreateDrawRequest.class);

        assertThat(result.title()).isEqualTo("Super Prize");
        assertThat(result.totalTickets()).isEqualTo(100);
        assertThat(result.endDate()).isEqualTo(OffsetDateTime.parse("2024-10-15T10:00:00Z"));
    }
}
