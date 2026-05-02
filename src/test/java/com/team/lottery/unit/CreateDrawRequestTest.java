package com.team.lottery.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team.lottery.draws.dto.CreateDrawRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CreateDrawRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test

    void shouldMaintainStateViaConstructor() {
        /*
        Должен корректно сохранять данные через конструктор и геттеры.
        * */

        String title = "Новогодний тираж";
        OffsetDateTime endDate = OffsetDateTime.of(2025, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        Integer totalTickets = 1000;


        CreateDrawRequest request = new CreateDrawRequest(title, endDate, totalTickets);


        assertThat(request.getTitle()).isEqualTo(title);
        assertThat(request.getEndDate()).isEqualTo(endDate);
        assertThat(request.getTotalTickets()).isEqualTo(totalTickets);
    }

    @Test
    void shouldSetProperties() {
        /*
        Должен корректно работать с сеттерами.
        * */

        CreateDrawRequest request = new CreateDrawRequest();
        String title = "Lotto 6/49";


        request.setTitle(title);
        request.setTotalTickets(500);


        assertThat(request.getTitle()).isEqualTo(title);
        assertThat(request.getTotalTickets()).isEqualTo(500);
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        /*
        Должен десериализоваться из JSON.
        * */

        String json = """
                {
                    "title": "Super Prize",
                    "endDate": "2024-10-15T10:00:00Z",
                    "totalTickets": 100
                }
                """;


        CreateDrawRequest result = objectMapper.readValue(json, CreateDrawRequest.class);


        assertThat(result.getTitle()).isEqualTo("Super Prize");
        assertThat(result.getTotalTickets()).isEqualTo(100);
        assertThat(result.getEndDate()).isEqualTo(OffsetDateTime.parse("2024-10-15T10:00:00Z"));
    }
}
